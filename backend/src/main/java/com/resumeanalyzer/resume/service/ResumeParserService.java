package com.resumeanalyzer.resume.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeanalyzer.ai.AiProviderResolver;
import com.resumeanalyzer.ai.model.ParsedResume;
import com.resumeanalyzer.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Extracts text from PDF resumes (Apache PDFBox) and derives structured fields.
 *
 * <p>Structured extraction follows a resilient strategy: the configured AI provider is the
 * primary extractor, and a regex-based fallback runs if the provider fails — so parsing
 * never hard-fails the upload flow.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParserService {

    private final AiProviderResolver aiProviderResolver;
    private final ObjectMapper objectMapper;
    private final RegexResumeExtractor regexExtractor;

    /** Extracts raw text from PDF bytes using PDFBox. */
    public String extractText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted()) {
                throw new BadRequestException("Encrypted PDFs are not supported");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);
            if (text == null || text.isBlank()) {
                throw new BadRequestException("No extractable text found in the PDF (is it a scanned image?)");
            }
            return text.trim();
        } catch (IOException e) {
            throw new BadRequestException("Failed to read PDF: " + e.getMessage());
        }
    }

    /** Produces a structured {@link ParsedResume}, falling back to regex on AI failure. */
    public ParsedResume parseStructured(String text) {
        try {
            return aiProviderResolver.current().analyzeResume(text);
        } catch (Exception e) {
            log.warn("AI resume extraction failed ({}); falling back to regex extractor", e.getMessage());
            return regexExtractor.extract(text);
        }
    }

    /** Serializes structured data to JSON for persistence. */
    public String toJson(ParsedResume parsed) {
        try {
            return objectMapper.writeValueAsString(parsed);
        } catch (Exception e) {
            log.warn("Failed to serialize parsed resume: {}", e.getMessage());
            return "{}";
        }
    }
}
