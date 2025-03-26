package uhlution.tomcat.redirect2root;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.catalina.util.ParameterMap;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.lang3.exception.ExceptionUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.Part;

public class MultipartParameterRequestWrapper extends HttpServletRequestWrapper {
	private static final Logger LOG = Logger.getLogger(MultipartParameterRequestWrapper.class.getName());
    private final Map<String, List<String>> parameters = new HashMap<>();
    private final Map<String, List<DiskFileItem>> fileItems = new HashMap<>();

    public MultipartParameterRequestWrapper(HttpServletRequest request, List<DiskFileItem> items) {
        super(request);
        
        // Copies the existing parameters (GET) into this request.
        request.getParameterMap().forEach((key, values) -> {
            parameters.put(key, new ArrayList<>(Arrays.asList(values)));
            request.getParameterMap().put(key, values);
        });
        
        // Cppies the POST-Parameters and Multipart-Data.
        Map<String, String[]> parameterMap =  request.getParameterMap();
        if (parameterMap instanceof ParameterMap<String, String[]>) {        	
        	((ParameterMap<String, String[]>)parameterMap).setLocked(false);
        }
        for (DiskFileItem item : items) {
            if (item.isFormField()) { // Falls kein Datei-Upload
                addParameter(item.getFieldName(), item.getString());
                parameterMap.put(item.getFieldName(), new String[] {item.getString()});
            } else {            	
            	fileItems.computeIfAbsent(item.getFieldName(), k -> new ArrayList<>()).add(item);
            }
        }
        if (parameterMap instanceof ParameterMap<String, String[]>) {        	
        	((ParameterMap<String, String[]>)parameterMap).setLocked(true);
        }
        
        // Sets the parts in the request via Reflection.
        setPartsOnRequest(request, getParts());
    }

    public void addParameter(String name, String value) {
        parameters.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }
    
    public void addParameters(String name, String[] values) {
        if (values != null) {
            parameters.computeIfAbsent(name, k -> new ArrayList<>()).addAll(Arrays.asList(values));
        }
    }    

    @Override
    public String getParameter(String name) {
        List<String> values = parameters.get(name);
        return (values != null && !values.isEmpty()) ? values.get(0) : super.getParameter(name);
    }

    @Override
    public String[] getParameterValues(String name) {
        List<String> values = parameters.get(name);
        return (values != null) ? values.toArray(new String[0]) : super.getParameterValues(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toArray(new String[0])));
    }

    @Override
    public Part getPart(String name) {
        List<DiskFileItem> list = fileItems.get(name);
        return (list != null && !list.isEmpty()) ? new FileItemPart(list.get(0)) : null;
    }

    @Override
    public Collection<Part> getParts() {
        return fileItems.values().stream()
            .flatMap(List::stream)
            .map(FileItemPart::new)
            .collect(Collectors.toList());
    }
    
    private void setPartsOnRequest(HttpServletRequest request, Collection<Part> parts) {
        try {
        	if (hasField(request, "request")) {        		
        		Field innerRequestFld = request.getClass().getDeclaredField("request");
        		innerRequestFld.setAccessible(true);
        		HttpServletRequest innerRequest = (HttpServletRequest)innerRequestFld.get(request);
        		innerRequestFld.setAccessible(false);

        		// Zugriff auf das `parts`-Feld in `org.apache.catalina.connector.Request`
        		Field partsField = innerRequest.getClass().getDeclaredField("parts");
        		partsField.setAccessible(true); // Zugriff auf private Felder ermöglichen
        		
        		// Setze die neue Parts-Liste in den Request
        		partsField.set(innerRequest, parts);
        	}
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOG.warning("Fehler beim Zugriff auf Tomcats parts-Feld via Reflection:\n" + ExceptionUtils.getStackTrace(e));
        }
    }

	private boolean hasField(HttpServletRequest request, String fieldName) throws NoSuchFieldException {
		try {
			return request.getClass().getDeclaredField(fieldName) != null;
		} catch (Exception e) {
			return false;
		}
	}    

}
