package com.benson.util;

import com.benson.dto.FormDataItem;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

public class FormDataToMultipartFileAdapter implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final byte[] content;

    public FormDataToMultipartFileAdapter(String name, FormDataItem formDataItem) {
        this.name = name;
        this.originalFilename = formDataItem.getFilename();
        this.contentType = formDataItem.getContentType();
        this.content = formDataItem.getValue().getBytes(); // 这里简化处理，实际应用可能需要根据内容类型进行适当处理
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() throws IOException {
        return content;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        try (FileOutputStream fos = new FileOutputStream(dest)) {
            fos.write(content);
        }
    }
}
