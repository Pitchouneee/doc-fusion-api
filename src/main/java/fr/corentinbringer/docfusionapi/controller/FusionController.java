package fr.corentinbringer.docfusionapi.controller;

import fr.corentinbringer.docfusionapi.service.ConversionService;
import fr.corentinbringer.docfusionapi.service.FusionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;

@RestController
@RequestMapping("/v1/documents")
@RequiredArgsConstructor
public class FusionController {

    private final ConversionService conversionService;
    private final FusionService fusionService;

    @PostMapping(value = "/generate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> generateDocument(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("data") String jsonData,
                                                   @RequestParam(value = "outputFormat", defaultValue = "pdf") String outputFormat) {
        try {
            ByteArrayInputStream modifiedDocxStream = fusionService.replacePlaceholders(file.getInputStream(), jsonData);

            byte[] convertedFile = conversionService.convert(modifiedDocxStream, outputFormat, file.getOriginalFilename());

            String mimeType = conversionService.getMimeType(outputFormat);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .body(convertedFile);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    ("Error: " + e.getMessage()).getBytes()
            );
        }
    }
}
