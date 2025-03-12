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

import static com.google.common.base.Splitter.on;
import static org.apache.commons.lang3.ObjectUtils.notEqual;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;

import java.io.IOException;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;

import jakarta.servlet.ServletException;

public class RedirectToRootValve extends ValveBase {

	public static final String ORIGINAL_REQUEST_URI = "originalRequestURI";
	public static final String ORIGINAL_CONTEXT_PATH = "originalContextPath";

	@Override
	public void invoke(Request request, Response response) throws IOException {
		String originalUri = request.getRequestURI();
		
		if (notEqual(originalUri, "/")) {
			request.setAttribute(ORIGINAL_REQUEST_URI, originalUri);
			
			String originalContext = evaluateOriginalContext(originalUri);
			request.setAttribute(ORIGINAL_CONTEXT_PATH, originalContext);

			String redirectUrl = evaluateRedirectUrl(originalUri, originalContext.length());

			request.getRequestDispatcher(redirectUrl);
		} else {
			try {
				getNext().invoke(request, response);
			} catch (IOException | ServletException e) {
				throw new RuntimeException(e);
			}
		}
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
		return substringBefore( "/" + on('/').splitToList(originalURI).get(1), "?");
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
