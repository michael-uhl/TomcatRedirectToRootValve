package uhlution.tomcat.redirect2root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uhlution.tomcat.redirect2root.RedirectToRootValve.ORIGINAL_CONTEXT_PATH;
import static uhlution.tomcat.redirect2root.RedirectToRootValve.ORIGINAL_REQUEST_URI;

import java.io.IOException;

import org.apache.catalina.Context;
import org.apache.catalina.Valve;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

public class RedirectToRootValveTest {

    private RedirectToRootValve valve;
    private Request mockRequest;
    private Response mockResponse;
    private Context mockContext;

    @BeforeEach
    void setUp() throws Exception {
        valve = new RedirectToRootValve();
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        mockContext = mock(Context.class);

        Valve mockNextValve = mock(Valve.class);
        doNothing().when(mockNextValve).invoke(any(Request.class), any(Response.class));
        valve.setNext(mockNextValve); // Sets the next the next valve.
        
        // Simulate the context.
        when(mockRequest.getContext()).thenReturn(mockContext);
        when(mockContext.getPath()).thenReturn("/");
        when(mockRequest.getContextPath()).thenReturn("");
    }

    @Test
    void requestAtRoot_NoRedirect() throws IOException {
        // Request already goes to Root ("/"), therefore NO forward.
        when(mockRequest.getRequestURI()).thenReturn("/");
        when(mockRequest.getContextPath()).thenReturn("/");

        // Valve execution.
        valve.invoke(mockRequest, mockResponse);

        // Check that NO forward happende.
        verify(mockResponse, never()).sendRedirect(anyString());
        verify(mockRequest, never()).getRequestDispatcher(anyString());
    }

    @ParameterizedTest
    @CsvSource({
        "/test, /",
        "/test/, /",
        "/test?q=1, /?q=1",
        "/test/?q=1, /?q=1",
        "/test/abc, /abc",
        "/test/abc/, /abc",
        "/test/abc?q=1, /abc?q=1",
        "/test/abc/?q=1, /abc/?q=1",
    })    
    void requestNotAtRoot_Redirect(String originalRequestURI, String forwardURI) throws IOException {
        // Request comes from an URI defined in the parameter.
        when(mockRequest.getRequestURI()).thenReturn(originalRequestURI);

        // ArgumentCaptor to capure the URL of the forward.
        ArgumentCaptor<String> redirectCaptor = ArgumentCaptor.forClass(String.class);

        // Valve execution.
        valve.invoke(mockRequest, mockResponse);

        // Verify the forward.
        verify(mockRequest).getRequestDispatcher(redirectCaptor.capture());

        // Check that the request is forwarded to "/".
        if (!forwardURI.equals(redirectCaptor.getValue()))
        	System.out.println("Request: " + originalRequestURI + ", Expected: " + forwardURI + ", Actual: " + redirectCaptor.getValue());
        
        assertEquals(forwardURI, redirectCaptor.getValue(), "Expected: " + forwardURI + ", Actual: " + redirectCaptor.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "/test", "/test?q=1", "/test/?q=1", "/test/abc", "/test/abc/", "/test/abc?q=1", "/test/abc/?q=1" })    
    void attributesAreSet(String originalRequestURI) throws IOException {
        // Request from original request URI.
        when(mockRequest.getRequestURI()).thenReturn(originalRequestURI);

        // Valve execution.
        valve.invoke(mockRequest, mockResponse);

        // Check if the attributes are set.
        verify(mockRequest).setAttribute(eq(ORIGINAL_CONTEXT_PATH), eq("/test"));
        verify(mockRequest).setAttribute(eq(ORIGINAL_REQUEST_URI), eq(originalRequestURI));
    }
}
