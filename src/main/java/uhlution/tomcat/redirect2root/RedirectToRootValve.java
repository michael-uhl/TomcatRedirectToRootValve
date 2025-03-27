/*
 * BSD 3-Clause License
 *
 * Copyright (c) 2025, Michael Uhl
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package uhlution.tomcat.redirect2root;

import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.splitPreserveAllTokens;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.fileupload2.jakarta.JakartaServletRequestContext;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

public class RedirectToRootValve extends ValveBase {

	public static final String ORIGINAL_REQUEST_URI = "originalRequestURI";
	public static final String ORIGINAL_CONTEXT_PATH = "originalContextPath";
	
	static {
        try (InputStream configStream = RedirectToRootValve.class.getResourceAsStream("/logging.properties")) {
            if (configStream != null) {
                LogManager.getLogManager().readConfiguration(configStream);
            } 
        } catch (IOException e) {
            e.printStackTrace();
        }		
	}
	private static final Logger LOG = Logger.getLogger(RedirectToRootValve.class.getName());

	@Override
	public void invoke(Request request, Response response) throws IOException {
		String originalUri = request.getRequestURI();
		
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("Processing Request '" + request.getRequestURI() + "'.");
		}
		
		if (notEqual(originalUri, "/") || request.getAttribute(ORIGINAL_CONTEXT_PATH) != null) {
			request.setAttribute(ORIGINAL_REQUEST_URI, originalUri);
			
			String originalContext = evaluateOriginalContext(originalUri);
			request.setAttribute(ORIGINAL_CONTEXT_PATH, originalContext);
			request.setAttribute("customerCtx", originalContext);

			String redirectUrl = evaluateRedirectUrl(originalUri, originalContext.length());

			LOG.warning("Redirect URL '" + redirectUrl + "'.");
			
			try {
				if (isMultipartRequest(request)) {
					wrapRequestWithCopiedParts(request, redirectUrl);			
				}
				
				RequestDispatcher requestDispatcher = request.getRequestDispatcher(redirectUrl);
				
				if (requestDispatcher == null) {
					LOG.warning("The requestDispatcher is null for request '" + redirectUrl + "'. Request is multipart? " + isMultipartRequest(request));
					return;
				}
				
				requestDispatcher.forward(request, response);
				return;
			} catch (ServletException | IOException e) {
				throw new RuntimeException(e);
			}
		} else if(request.getDispatcherType() == DispatcherType.FORWARD) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine("Request zu '" + request.getRequestURI() + "' wird nicht weiter geforwardet.");
			}
		} else {
			try {
				getNext().invoke(request, response);
			} catch (IOException | ServletException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void wrapRequestWithCopiedParts(Request request, String servletPath) throws IOException, ServletException {
        HttpServletRequest httpReq = request.getRequest();
        
        // 📌 Extracts all query parameters manually from the URL (GET-Parameter).
        //Map<String, List<String>> originalParams = extractQueryParameters(httpReq);

        // 📌 Parses Multipart-Data using JakartaServletFileUpload.
        DiskFileItemFactory factory = DiskFileItemFactory.builder()
                .setBufferSize(1024 * 1024)
                .get();
        
        JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload =
                new JakartaServletFileUpload<>(factory);
        List<DiskFileItem> items = upload.parseRequest(new JakartaServletRequestContext(request));

        // 📌 Creates MultipartParameterRequestWrapper with original parameters (POST) and FileItems.
        MultipartParameterRequestWrapper wrappedRequest = new MultipartParameterRequestWrapper(httpReq, items);

        // 📌 Sets wrapper as request object.
        request.setRequest(wrappedRequest);
	}

	private boolean isMultipartRequest(Request request) {
		String ct = request.getRequest().getContentType();
		return ct != null && ct.toLowerCase().startsWith("multipart/");
	}

	private String evaluateRedirectUrl(String originalUri, int originalContextLen) {
		String redirectUrl = originalUri.substring(originalContextLen);
		if (isBlank(redirectUrl)) {
			redirectUrl = "/";
		}
		if (!redirectUrl.startsWith("/")) {
			redirectUrl = "/" + redirectUrl;
		}
		if (redirectUrl.endsWith("/") && notEqual(redirectUrl, "/")) {
			redirectUrl = redirectUrl.substring(0, redirectUrl.length() - 1);
		}
		return redirectUrl;
	}

	private String evaluateOriginalContext(String originalURI) {
	    // Zerlege die URI in Token, wobei leere Tokens erhalten bleiben (z.B. vor dem ersten '/')
	    String[] tokens = splitPreserveAllTokens(originalURI, '/');
	    
	    // Prüfe, ob mindestens zwei Tokens vorhanden sind (das erste Token ist ggf. leer)
	    if (tokens.length < 2) {
	        return "";
	    }
	    // Entferne aus dem ersten relevanten Token (z.B. "abc?param=1") den Query-Teil
	    String context = substringBefore(tokens[1], "?");
	    
	    return "/" + context;
	}

	@Override
	protected void startInternal() throws LifecycleException {
		super.startInternal();
	}

	@Override
	protected void stopInternal() throws LifecycleException {
		super.stopInternal();
	}

}
