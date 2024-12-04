package fr.corentinbringer.docfusionapi.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;

@Service
@RequiredArgsConstructor
public class ConversionService {

    @Value("${jodconverter.url}")
    private String jodConverterUrl;

    private final RestTemplate restTemplate;

    public byte[] convert(ByteArrayInputStream inputFileStream, String outputFormat, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("data", new ByteArrayResource(inputFileStream.readAllBytes()) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        String conversionUrl = jodConverterUrl + "/" + outputFormat;

        ResponseEntity<byte[]> response = restTemplate.exchange(conversionUrl, HttpMethod.POST, requestEntity, byte[].class);

        return response.getBody();
    }

    public String getMimeType(String outputFormat) {
        return switch (outputFormat.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
