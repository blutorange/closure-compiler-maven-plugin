package com.github.blutorange.maven.plugin.closurecompiler.common;

import static com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper.absoluteFileToCanonicalFile;
import static com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper.getAbsoluteFile;
import static com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper.relativizePath;
import static com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper.relativizeRelativePath;
import static com.github.blutorange.maven.plugin.closurecompiler.common.FileHelper.startsWithBom;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.separatorsToUnix;
import static org.apache.commons.lang3.StringUtils.startsWith;

import com.github.blutorange.maven.plugin.closurecompiler.plugin.HtmlUpdate;
import com.github.blutorange.maven.plugin.closurecompiler.shared.HtmlUpdateConfig;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Range;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

/**
 * Updates a given set of HTML files and sets the source of the configured script elements the generated files.
 *
 * @since 2.32.0
 */
public final class HtmlUpdater {
    private final Log log;
    private final HtmlUpdateConfig updateConfig;

    /**
     * Creates a new update for a minify plugin.
     *
     * @param log The Maven logger.
     * @param updateConfig Plugin-global configuration for the HTML update operation.
     */
    public HtmlUpdater(Log log, HtmlUpdateConfig updateConfig) {
        this.log = log;
        this.updateConfig = updateConfig;
    }

    private static Elements toElements(Element element) {
        return element != null ? new Elements(element) : new Elements();
    }

    private static boolean isHtml(File file) {
        return isHtml(file.getName());
    }

    private static boolean isHtml(String file) {
        final var extension = getExtension(file);
        return "html".equals(extension) || "htm".equals(extension);
    }

    private static String formatPosition(Range.Position position) {
        return position.lineNumber() + ":" + position.columnNumber();
    }

    private static String toWebPath(String relativePath) {
        final var unixRelativePath = separatorsToUnix(relativePath);
        return new File(unixRelativePath).isAbsolute() || startsWith(unixRelativePath, ".")
                ? unixRelativePath
                : "./" + unixRelativePath;
    }

    /**
     * Update all HTML files with the given generated files.
     *
     * @param processingResults Results of the minify plugin, with the generated script files.
     * @throws MojoExecutionException When the HTML files could not be updated.
     */
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
        final var encoding = Charset.forName(htmlUpdate.getEncoding());
        final var htmlDocument = parseHtmlFile(htmlFile, encoding);
        if (htmlDocument == null) {
            return;
        }
        final var relativeHtmlPath = relativizeHtmlFile(htmlUpdate, htmlFile);
        final var modifications = new ArrayList<TextFileModification>();
        for (final var processingResult : processingResults) {
            if (processingResult.getOutput() == null) {
                continue;
            }
            modifications.addAll(processProcessingResult(processingResult, htmlUpdate, htmlDocument, relativeHtmlPath));
        }
        applyModifications(htmlFile, encoding, modifications);
    }

    private void applyModifications(File htmlFile, Charset encoding, List<TextFileModification> modifications)
            throws MojoExecutionException {
        if (modifications.isEmpty()) {
            log.info("HTML file <" + htmlFile + "> is already up-to-date");
        } else {
            try {
                final var adjustedModifications = adjustModifications(htmlFile, encoding, modifications);
                final var hasChanges = TextFileModifications.applyAndWrite(htmlFile, encoding, adjustedModifications);
                if (hasChanges) {
                    log.info("Updated HTML file <" + htmlFile + ">");
                }
            } catch (final Exception e) {
                throw new MojoExecutionException("Failed to apply modifications to <" + htmlFile + ">", e);
            }
        }
    }

    private List<TextFileModification> adjustModifications(
            File htmlFile, Charset encoding, List<TextFileModification> modifications) throws IOException {
        final var startsWithBom = startsWithBom(htmlFile, encoding);
        if (startsWithBom) {
            return modifications.stream()
                    .map(modification -> modification.withOffset(1))
                    .collect(toList());
        } else {
            return modifications;
        }
    }

    private List<TextFileModification> processProcessingResult(
            ProcessingResult processingResult, HtmlUpdate htmlUpdate, Document htmlDocument, String relativeHtmlPath)
            throws MojoExecutionException {
        final var scriptFile = absoluteFileToCanonicalFile(processingResult.getOutput());
        final var relativeScriptPath = relativizeScriptFile(htmlUpdate, scriptFile);
        final var resolvedSourcePath = resolveSourcePath(htmlUpdate, relativeHtmlPath, relativeScriptPath, scriptFile);
        return updateHtmlFile(htmlUpdate, htmlDocument, resolvedSourcePath);
    }

    private String resolveSourcePath(
            HtmlUpdate htmlUpdate, String relativeHtmlPath, String relativeScriptPath, File scriptFile) {
        final var sourcePath = StringUtils.defaultIfBlank(updateConfig.getHtmlSourcePath(), htmlUpdate.getSourcePath());
        if (sourcePath.isBlank()) {
            final var relativeHtmlDirPath = new File(relativeHtmlPath).getParentFile();
            final var relativePath = relativizeRelativePath(relativeHtmlDirPath, new File(relativeScriptPath));
            return toWebPath(relativePath);
        } else {
            final var interpolator = new FilenameInterpolator(sourcePath);
            final var usePhysicalRoot = resolveUsePhysicalRoot(htmlUpdate);
            final var scriptBaseDir = usePhysicalRoot
                    ? scriptFile
                    : getAbsoluteFile(updateConfig.getHtmlScriptRoot(), htmlUpdate.getScriptRoot());
            final var path = interpolator.interpolateRelative(scriptFile, scriptBaseDir);
            return separatorsToUnix(path);
        }
    }

    private String relativizeHtmlFile(HtmlUpdate htmlUpdate, File htmlFile) throws MojoExecutionException {
        final var usePhysicalRoot = resolveUsePhysicalRoot(htmlUpdate);
        if (usePhysicalRoot) {
            return htmlFile.getPath();
        }
        final var htmlRoot = getAbsoluteFile(updateConfig.getHtmlRoot(), htmlUpdate.getRoot());
        return relativizePath(htmlRoot, htmlFile);
    }

    private String relativizeScriptFile(HtmlUpdate htmlUpdate, File scriptFile) throws MojoExecutionException {
        final var usePhysicalRoot = resolveUsePhysicalRoot(htmlUpdate);
        if (usePhysicalRoot) {
            return scriptFile.getPath();
        }
        final var htmlScriptRoot = getAbsoluteFile(updateConfig.getHtmlScriptRoot(), htmlUpdate.getScriptRoot());
        return relativizePath(htmlScriptRoot, scriptFile);
    }

    private boolean resolveUsePhysicalRoot(HtmlUpdate htmlUpdate) {
        final var resolved =
                ObjectUtils.defaultIfNull(htmlUpdate.isUsePhysicalRoot(), updateConfig.isHtmlUsePhysicalRoot());
        return Boolean.TRUE.equals(resolved);
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
            if (log.isDebugEnabled()) {
                log.debug("Updating script element " + script + " at position "
                        + formatPosition(script.sourceRange().start()));
            }
            for (final var attributeName : htmlUpdate.getAttributes()) {
                final var isHtml = isHtml(document.location());
                final var setAttribute = HtmlModifier.setAttribute(script, attributeName, sourcePath, isHtml);
                final var clearTextContent = HtmlModifier.clearTextContent(script);
                if (setAttribute != null) {
                    modifications.add(setAttribute);
                }
                if (clearTextContent != null) {
                    modifications.add(clearTextContent);
                }
            }
        }
        return modifications;
    }

    private Elements findScripts(HtmlUpdate htmlUpdate, Document document) {
        final var selector = htmlUpdate.getScripts();
        if (selector.isEmpty()) {
            final var scripts = document.getElementsByTag("SCRIPT");
            return scripts.isEmpty() ? scripts : new Elements(scripts.get(0));
        }
        final var colon = selector.indexOf(':');
        if (colon < 1) {
            log.warn("Invalid selector <" + selector + ">, must starts with a type (<id:>, <css:>, or <xpath:>)");
            return new Elements();
        }
        final var type = selector.substring(0, colon);
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
        final var namespace = isHtml(file) ? "http://www.w3.org/1999/xhtml" : "http://www.w3.org/XML/1998/namespace";
        parser.setTrackErrors(100);
        parser.setTrackPosition(true);
        // Allow self-closing <script /> tags
        parser.tagSet().add(new Tag("script", namespace).set(Tag.SelfClose));
        try {
            final var document = Jsoup.parse(file, encoding.name(), file.toURI().toASCIIString(), parser);
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

    private List<File> resolveHtmlFiles(HtmlUpdate htmlUpdate) {
        final var base = getAbsoluteFile(updateConfig.getHtmlDir(), htmlUpdate.getDir());
        final var htmlFiles = htmlUpdate.getFiles().getFiles(base);
        if (htmlFiles.isEmpty()) {
            log.warn("Did not find any HTML files to update in directory <" + base + "> with " + htmlUpdate.getFiles());
        }
        return htmlFiles;
    }
}
