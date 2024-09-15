package com.github.blutorange.maven.plugin.closurecompiler.common;

import com.github.blutorange.maven.plugin.closurecompiler.plugin.HtmlUpdate;
import com.github.blutorange.maven.plugin.closurecompiler.plugin.HtmlUpdateConfig;
import com.github.blutorange.maven.plugin.closurecompiler.plugin.MojoMetadata;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;

public final class HtmlUpdater {
    private final Log log;
    private final HtmlUpdateConfig updateConfig;

    public HtmlUpdater(MojoMetadata mojoMeta, HtmlUpdateConfig updateConfig) {
        this.log = mojoMeta.getLog();
        this.updateConfig = updateConfig;
    }

    private static Elements toElements(Element element) {
        return element != null ? new Elements(element) : new Elements();
    }

    public void process(List<ProcessingResult> processingResults) throws MojoExecutionException {
        for (final var htmlUpdate : updateConfig.getHtmlUpdates()) {
            processHtmlUpdate(processingResults, htmlUpdate);
        }
    }

    private void processHtmlUpdate(List<ProcessingResult> processingResults, HtmlUpdate htmlUpdate)
            throws MojoExecutionException {
        final var htmlFiles = resolveHtmlFiles(htmlUpdate);
        for (final var htmlFile : htmlFiles) {
            processHtmlFile(processingResults, htmlUpdate, htmlFile);
        }
    }

    private void processHtmlFile(List<ProcessingResult> processingResults, HtmlUpdate htmlUpdate, File htmlFile)
            throws MojoExecutionException {
        log.debug("Processing HTML file <" + htmlFile + ">");
        final var encoding = Charset.forName(htmlUpdate.getHtmlEncoding());
        final var htmlDocument = parseHtmlFile(htmlFile, encoding);
        if (htmlDocument == null) {
            return;
        }
        final var relativeHtmlPath = relativizeHtmlFile(htmlUpdate, htmlFile);
        final var modifications = new ArrayList<TextFileModification>();
        for (final var processingResult : processingResults) {
            modifications.addAll(processProcessingResult(processingResult, htmlUpdate, htmlDocument, relativeHtmlPath));
        }
        applyModifications(htmlFile, encoding, modifications);
    }

    private void applyModifications(File htmlFile, Charset encoding, ArrayList<TextFileModification> modifications)
            throws MojoExecutionException {
        if (modifications.isEmpty()) {
            log.info("HTML file <" + htmlFile + "> is already up-to-date");
        } else {
            log.info("Updating HTML file <" + htmlFile + ">");
            try {
                TextFileModifications.applyAndWrite(htmlFile, encoding, modifications);
            } catch (final Exception e) {
                throw new MojoExecutionException("Failed to apply modifications to <" + htmlFile + ">", e);
            }
        }
    }

    private List<TextFileModification> processProcessingResult(
            ProcessingResult processingResult, HtmlUpdate htmlUpdate, Document htmlDocument, String relativeHtmlPath)
            throws MojoExecutionException {
        final var scriptFile = FileHelper.absoluteFileToCanonicalFile(processingResult.getOutput());
        final var relativeScriptPath = relativizeScriptFile(htmlUpdate, scriptFile);
        final var resolvedSourcePath = resolveSourcePath(htmlUpdate, relativeHtmlPath, relativeScriptPath, scriptFile);
        return updateHtmlFile(htmlUpdate, htmlDocument, resolvedSourcePath);
    }

    private String resolveSourcePath(
            HtmlUpdate htmlUpdate, String relativeHtmlPath, String relativeScriptPath, File scriptFile) {
        if (htmlUpdate.getSourcePath().isEmpty()) {
            return FileHelper.relativizePath(new File(relativeHtmlPath).getParentFile(), new File(relativeScriptPath));
        } else {
            final var interpolator = new FilenameInterpolator(htmlUpdate.getSourcePath());
            final var scriptBaseDir = "/".equals(htmlUpdate.getHtmlScriptRoot())
                    ? scriptFile
                    : FileHelper.getAbsoluteFile(updateConfig.getHtmlScriptRoot(), htmlUpdate.getHtmlScriptRoot());
            return interpolator.interpolateRelative(scriptFile, scriptBaseDir);
        }
    }

    private String relativizeHtmlFile(HtmlUpdate htmlUpdate, File htmlFile) throws MojoExecutionException {
        if ("/".equals(htmlUpdate.getHtmlRoot())) {
            return htmlFile.getPath();
        }
        final var htmlRoot = FileHelper.getAbsoluteFile(updateConfig.getHtmlRoot(), htmlUpdate.getHtmlRoot());
        return FileHelper.relativizePath(htmlRoot, htmlFile);
    }

    private String relativizeScriptFile(HtmlUpdate htmlUpdate, File scriptFile) throws MojoExecutionException {
        if ("/".equals(htmlUpdate.getHtmlScriptRoot())) {
            return scriptFile.getPath();
        }
        final var htmlScriptRoot =
                FileHelper.getAbsoluteFile(updateConfig.getHtmlScriptRoot(), htmlUpdate.getHtmlScriptRoot());
        return FileHelper.relativizePath(htmlScriptRoot, scriptFile);
    }

    private List<TextFileModification> updateHtmlFile(HtmlUpdate htmlUpdate, Document document, String sourcePath) {
        final var scripts = findScripts(htmlUpdate, document);
        if (scripts.isEmpty()) {
            log.warn("Did not find any script elements to update for document <" + document.location()
                    + "> via selector <" + htmlUpdate.getScripts() + ">");
            return List.of();
        }
        final var modifications = new ArrayList<TextFileModification>();
        for (final var script : scripts) {
            for (final var attributeName : htmlUpdate.getAttributes()) {
                final var isHtml = isHtml(document.location());
                final var modification = HtmlModifier.setAttribute(script, attributeName, sourcePath, isHtml);
                modifications.add(modification);
            }
        }
        return modifications;
    }

    private Elements findScripts(HtmlUpdate htmlUpdate, Document document) {
        final var selector = htmlUpdate.getScripts();
        if (selector.isEmpty()) {
            return document.getElementsByTag("SCRIPT");
        }
        final var colon = selector.indexOf(':');
        if (colon < 1) {
            log.warn("Invalid selector <" + selector + ">, must starts with a type (<id:>, <css:>, or <xpath:>)");
            return new Elements();
        }
        final var type = selector.substring(0, colon - 1);
        final var value = selector.substring(colon + 1);
        switch (type) {
            case "id":
                return toElements(document.getElementById(value));
            case "css":
                return findByCssQuery(document, value);
            case "xpath":
                return findByXPath(document, value);
            default:
                log.warn("Invalid selector <" + selector + ">, type must be one of 'id', 'css', or 'xpath'");
                return new Elements();
        }
    }

    private Elements findByXPath(Document document, String xPath) {
        try {
            return document.selectXpath(xPath);
        } catch (final Exception e) {
            log.error("Could not select element by XPath <" + xPath + "> in document <" + document.location() + ">", e);
            return new Elements();
        }
    }

    private Elements findByCssQuery(Document document, String cssQuery) {
        try {
            return document.select(cssQuery);
        } catch (final Exception e) {
            log.error(
                    "Could not select element by CSS query <" + cssQuery + "> in document <" + document.location()
                            + ">",
                    e);
            return new Elements();
        }
    }

    private Document parseHtmlFile(File file, Charset encoding) {
        final var parser = isHtml(file) ? Parser.htmlParser() : Parser.xmlParser();
        parser.setTrackErrors(100);
        parser.setTrackPosition(true);
        try {
            final var document = Jsoup.parse(file, encoding.name(), "", parser);
            for (final var error : parser.getErrors()) {
                log.error("Encountered error while parsing <" + file + "> at position <" + error.getCursorPos() + "> : "
                        + error.getErrorMessage());
            }
            return document;
        } catch (final Exception e) {
            log.error("Could not update (X)HTML file, file <" + file + "could not be parsed", e);
            return null;
        }
    }

    private static boolean isHtml(File file) {
        return isHtml(file.getName());
    }

    private static boolean isHtml(String file) {
        final var extension = FilenameUtils.getExtension(file);
        return "html".equals(extension) || "htm".equals(extension);
    }

    private List<File> resolveHtmlFiles(HtmlUpdate htmlUpdate) {
        final var base = FileHelper.getAbsoluteFile(updateConfig.getHtmlDir(), htmlUpdate.getHtmlDir());
        final var htmlFiles = htmlUpdate.getHtmlFiles().getFiles(base);
        if (htmlFiles.isEmpty()) {
            log.warn("Did not find any HTML files to update in directory <" + base + "> " + htmlUpdate.getHtmlFiles());
        }
        return htmlFiles;
    }
}
