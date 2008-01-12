package org.carrot2.source.yahoo;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.lang.StringEscapeUtils;
import org.carrot2.core.Document;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * XML content handler for parsing Yahoo! search response.
 */
final class XMLResponseParser implements ContentHandler
{
    /** Search response object. */
    public SearchResponse response;

    /** Currently parsed document. */
    private Document document;

    /** Parsed element stack. */
    private ArrayList<String> stack = new ArrayList<String>();

    /** String builder for assembling content. */
    private StringBuilder buffer = new StringBuilder();

    /** An error occurred. */
    private boolean error;
    private StringBuilder errorText;

    /*
     * 
     */
    @Override
    public void startDocument() throws SAXException
    {
        this.error = false;
        this.response = null;
        this.stack.clear();

        cleanup();
    }

    /*
     * 
     */
    private void cleanup()
    {
        this.document = null;
        this.buffer.setLength(0);
    }

    /*
     * 
     */
    @Override
    public void endDocument() throws SAXException
    {
        if (error) {
            throw new SAXException(new IOException("Yahoo! service error: " + errorText.toString()));
        }
    }

    /*
     * 
     */
    @Override
    public void startElement(String uri, String localName, String qname,
        Attributes attributes) throws SAXException
    {
        buffer.setLength(0);
        if (stack.size() == 0 && "ResultSet".equals(localName))
        {
            String tmp;

            long totalResults = 0;
            int resultsReturned = 0;
            int firstResultPosition = 0;

            tmp = attributes.getValue("totalResultsAvailable");
            if (tmp != null)
            {
                totalResults = Long.parseLong(tmp);
            }

            tmp = attributes.getValue("totalResultsReturned");
            if (tmp != null)
            {
                resultsReturned = Integer.parseInt(tmp);
            }

            tmp = attributes.getValue("firstResultPosition");
            if (tmp != null)
            {
                firstResultPosition = Integer.parseInt(tmp);
            }

            this.response = new SearchResponse(totalResults, firstResultPosition, resultsReturned);
        }
        else if (stack.size() == 0 && "Error".equals(localName))
        {
            this.error = true;
            errorText = new StringBuilder();
        }
        else if (stack.size() > 1 && error)
        {
            errorText.append(localName + ": ");
        }
        else if ("Result".equals(localName))
        {
            this.document = new Document();
        }

        stack.add(localName);
    }

    /*
     * 
     */
    @Override
    public void endElement(String uri, String localName, String qname)
        throws SAXException
    {
        if (error)
        {
            errorText.append(buffer);
            errorText.append("\n");
        }
        else if (stack.size() == 2 && "Result".equals(localName))
        {
            // New result parsed. Push it.
            this.response.results.add(document);

            // Cleanup for the next element.
            cleanup();
        }
        else if (stack.size() == 3 && "Result".equals(stack.get(1)))
        {
            // Yahoo! returns double-encoded entities in its response (it
            // return escaped HTML), so reparse it.
            final String text = StringEscapeUtils.unescapeHtml(buffer.toString());
            buffer.setLength(0);

            // Recognize special fields and translate them.
            if ("Title".equals(localName))
            {
                document.addField(Document.TITLE, text);
            }
            else if ("Summary".equals(localName))
            {
                document.addField(Document.SUMMARY, text);
            }
            else if ("Url".equals(localName))
            {
                document.addField(Document.CONTENT_URL, text);
            }
            else 
            {
                // All other fields go directly in the document.
                document.addField(localName, text);
            }
        }

        stack.remove(stack.size() - 1);
    }

    /*
     * 
     */
    @Override
    public void characters(char [] chars, int start, int length) throws SAXException
    {
        buffer.append(chars, start, length);
    }

    /*
     * 
     */
    @Override
    public void setDocumentLocator(Locator locator)
    {
        // Empty.
    }

    /*
     * 
     */
    @Override
    public void startPrefixMapping(String arg0, String arg1) throws SAXException
    {
        // Empty.
    }

    /*
     * 
     */
    @Override
    public void endPrefixMapping(String arg0) throws SAXException
    {
        // Empty.
    }

    /*
     * 
     */
    @Override
    public void ignorableWhitespace(char [] whsp, int start, int length)
        throws SAXException
    {
    }

    /*
     * 
     */
    @Override
    public void processingInstruction(String name, String value) throws SAXException
    {
    }

    /*
     * 
     */
    @Override
    public void skippedEntity(String entity) throws SAXException
    {
    }
}
