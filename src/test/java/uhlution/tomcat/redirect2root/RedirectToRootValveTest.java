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

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class RedirectToRootValveTest {

    private RedirectToRootValve valve;
    private Request mockRequest;
    private Response mockResponse;
    private RequestDispatcher mockRequestDispatcher;
    private HttpServletRequest mockHttpServletReq;
    private HttpServletResponse mockHttpServletResp;
    private Context mockContext;

    @BeforeEach
    void setUp() throws Exception {
        valve = new RedirectToRootValve();
        mockRequest = mock(Request.class);
        mockResponse = mock(Response.class);
        mockContext = mock(Context.class);
        mockRequestDispatcher = mock(RequestDispatcher.class);
        mockHttpServletReq = mock(HttpServletRequest.class);
        mockHttpServletResp = mock(HttpServletResponse.class);

        Valve mockNextValve = mock(Valve.class);
        doNothing().when(mockNextValve).invoke(any(Request.class), any(Response.class));
        valve.setNext(mockNextValve); // Sets the next the next valve.
        
        // Simulate the context.
        when(mockRequest.getContext()).thenReturn(mockContext);
        when(mockRequest.getRequestDispatcher(anyString())).thenReturn(mockRequestDispatcher);
        when(mockRequest.getRequest()).thenReturn(mockHttpServletReq);
        when(mockResponse.getResponse()).thenReturn(mockHttpServletResp);
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
