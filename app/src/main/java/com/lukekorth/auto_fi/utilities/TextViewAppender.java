package com.lukekorth.auto_fi.utilities;

import android.widget.TextView;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.Layout;

/**
 * An appender that redirects all logging requests to a {@link android.widget.TextView}
 */
public class TextViewAppender extends AppenderBase<ILoggingEvent> {

    private PatternLayoutEncoder encoder = null;
    private PatternLayoutEncoder tagEncoder = null;
    private TextView textView;

    /**
     * @param textView the {@link TextView} to write to.
     */
    public TextViewAppender(TextView textView) {
        this.textView = textView;
    }

    /**
     * Checks that required parameters are set, and if everything is in order,
     * activates this appender.
     */
    @Override
    public void start() {
        if (this.textView == null) {
            addError("No TextView set");
            return;
        }

        if (this.encoder == null || this.encoder.getLayout() == null) {
            addError("No layout set for the appender named [" + name + "].");
            return;
        }

        // tag encoder is optional but needs a layout
        if (this.tagEncoder != null) {
            final Layout<?> layout = this.tagEncoder.getLayout();

            if (layout == null) {
                addError("No tag layout set for the appender named [" + name + "].");
                return;
            }

            // prevent stack traces from showing up in the tag
            // (which could lead to very confusing error messages)
            if (layout instanceof PatternLayout) {
                String pattern = this.tagEncoder.getPattern();
                if (!pattern.contains("%nopex")) {
                    this.tagEncoder.stop();
                    this.tagEncoder.setPattern(pattern + "%nopex");
                    this.tagEncoder.start();
                }

                PatternLayout tagLayout = (PatternLayout) layout;
                tagLayout.setPostCompileProcessor(null);
            }
        }

        super.start();
    }

    /**
     * Writes an event to the {@link TextView} as a new line.
     *
     * @param event the event to be logged
     */
    public void append(ILoggingEvent event) {
        if (!isStarted()) {
            return;
        }

        switch (event.getLevel().levelInt) {
            case Level.ALL_INT:
            case Level.TRACE_INT:
            case Level.DEBUG_INT:
            case Level.INFO_INT:
            case Level.WARN_INT:
            case Level.ERROR_INT:
                this.textView.append(this.encoder.getLayout().doLayout(event));
                break;
            case Level.OFF_INT:
            default:
                break;
        }
    }

    /**
     * Gets the pattern-layout encoder for this appender's message
     *
     * @return the pattern-layout encoder
     */
    public PatternLayoutEncoder getEncoder() {
        return this.encoder;
    }

    /**
     * Sets the pattern-layout encoder for this appender's message
     *
     * @param encoder the pattern-layout encoder
     */
    public void setEncoder(PatternLayoutEncoder encoder) {
        this.encoder = encoder;
    }

    /**
     * Gets the tag string of a logging event
     * @param event logging event to evaluate
     * @return the tag string
     */
    private String getTag(ILoggingEvent event) {
        return (this.tagEncoder != null) ? this.tagEncoder.getLayout().doLayout(event) : event.getLoggerName();
    }
}
