package uhlution.tomcat.redirect2root;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.fileupload2.core.DiskFileItem;

import jakarta.servlet.http.Part;

public class FileItemPart implements Part {
    private final DiskFileItem item;
    public FileItemPart(DiskFileItem item) { this.item = item; }
    @Override public InputStream getInputStream() throws IOException { return item.getInputStream(); }
    @Override public String getContentType() { return item.getContentType(); }
    @Override public String getName() { return item.getFieldName(); }
    @Override public long getSize() { return item.getSize(); }
    @Override public String getSubmittedFileName() { return item.getName(); }
    @Override public void write(String filename) throws IOException { item.write(Path.of("", filename)); }
    @Override public void delete() throws IOException { item.delete(); }
    @Override public String getHeader(String name) { return item.getHeaders().getHeader(name); }
    public void write(Path file) throws IOException { Files.copy(getInputStream(), file, StandardCopyOption.REPLACE_EXISTING); }

    @Override 
    public Collection<String> getHeaderNames() { 
    	List<String> result = new ArrayList<>();
    	Iterator<String> headerNames = item.getHeaders().getHeaderNames();
    	while (headerNames.hasNext()) {
    		result.add(headerNames.next());
    	}
    	
    	return result;
    }
    
    @Override 
    public Collection<String> getHeaders(String name) { 
    	List<String> result = new ArrayList<>();
    	Iterator<String> headerNames = item.getHeaders().getHeaders(name);
    	while (headerNames.hasNext()) {
    		result.add(headerNames.next());
    	}
    	
    	return result;
    }    
}
