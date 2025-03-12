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
