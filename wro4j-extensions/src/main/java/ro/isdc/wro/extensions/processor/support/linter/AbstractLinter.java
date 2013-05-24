/*
 * Copyright wro4j@2011
 */
package ro.isdc.wro.extensions.processor.support.linter;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ro.isdc.wro.WroRuntimeException;
import ro.isdc.wro.extensions.locator.WebjarResourceLocatorFactory;
import ro.isdc.wro.extensions.processor.support.csslint.CssLint;
import ro.isdc.wro.extensions.script.RhinoScriptBuilder;
import ro.isdc.wro.model.resource.locator.factory.ResourceLocatorFactory;
import ro.isdc.wro.util.StopWatch;
import ro.isdc.wro.util.WroUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


/**
 * Encapsulates common code for utilities like jsHint or jsLint.
 *
 * @author Alex Objelean
 * @created 19 Sept 2011
 * @since 1.4.2
 */
public abstract class AbstractLinter {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractLinter.class);
  private ResourceLocatorFactory webjarLocatorFactory;
  private final OptionsBuilder optionsBuilder = new OptionsBuilder();
  /**
   * Options to apply to js hint processing
   */
  private String options;

  /**
   * Initialize script builder for evaluation.
   */
  private RhinoScriptBuilder initScriptBuilder() {
    try {
      // reusing the scope doesn't work here. Get the following error: TypeError: Cannot find function create in object
      // function Object() { [native code for Object.Object, arity=1] }
      // TODO investigate why
      return RhinoScriptBuilder.newChain().evaluateChain(getScriptAsStream(), "linter.js");
    } catch (final IOException e) {
      throw new WroRuntimeException("Failed reading init script", e);
    }
  }

  /**
   * @return {@link ResourceLocatorFactory} instance to retrieve webjars.
   */
  protected final ResourceLocatorFactory getWebjarLocatorFactory() {
    if (webjarLocatorFactory == null) {
      webjarLocatorFactory = new WebjarResourceLocatorFactory();
    }
    return webjarLocatorFactory;
  }

  /**
   * @return the stream of the linter script. Override this method to provide a different script version.
   * @throws IOException if the stream is invalid or unavailable.
   */
  protected abstract InputStream getScriptAsStream() throws IOException;

  /**
   * Validates a js using jsHint and throws {@link LinterException} if the js is invalid. If no exception is thrown, the
   * js is valid.
   *
   * @param data
   *          js content to process.
   */
  public void validate(final String data)
      throws LinterException {
    final StopWatch watch = new StopWatch();
    watch.start("init");
    final RhinoScriptBuilder builder = initScriptBuilder();
    watch.stop();
    watch.start("lint");
    final String packIt = buildLinterScript(WroUtil.toJSMultiLineString(data), getOptions());
    final boolean valid = Boolean.parseBoolean(builder.evaluate(packIt, "check").toString());
    if (!valid) {
      final String json = builder.addJSON().evaluate(String.format("JSON.stringify(JSON.decycle(%s.errors))", getLinterName()),
          "stringify errors").toString();
      LOG.debug("json {}", json);
      final Type type = new TypeToken<List<LinterError>>() {}.getType();
      final List<LinterError> errors = new Gson().fromJson(json, type);
      LOG.debug("errors {}", errors);
      throw new LinterException().setErrors(errors);
    }
    LOG.debug("result: {}", valid);
    watch.stop();
    LOG.debug(watch.prettyPrint());
  }

  /**
   * @return the name of the function used to perform the lint operation.
   */
  protected abstract String getLinterName();

  /**
   * TODO this method is duplicated in {@link CssLint}. Extract and reuse it.
   *
   * @param data
   *          script to process.
   * @param options
   *          options to set as true
   * @return Script used to pack and return the packed result.
   */
  private String buildLinterScript(final String data, final String options) {
    return String.format("%s(%s,%s);", getLinterName(), data, optionsBuilder.buildFromCsv(options));
  }

  /**
   * @param options
   *          the options to set
   */
  public AbstractLinter setOptions(final String... options) {
    this.options = StringUtils.join(options, ',');
    LOG.debug("setOptions: {}", this.options);
    return this;
  }


  /**
   * @return an options as CSV.
   */
  private String getOptions() {
    if (options == null) {
      options = createDefaultOptions();
    }
    return options;
  }

  /**
   * @return default options to use for linting.
   */
  protected String createDefaultOptions() {
    return "";
  }
}
