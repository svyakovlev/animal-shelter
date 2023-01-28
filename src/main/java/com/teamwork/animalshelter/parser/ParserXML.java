package com.teamwork.animalshelter.parser;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class ParserXML {
    private final SAXParser parser;
    private final ParserHandler handler;

    public ParserXML() {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            this.parser = factory.newSAXParser();
            this.handler = new ParserHandler();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    Element parse(String filePath) {
        File file = new File(filePath);
        try {
            parser.parse(file, handler);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return handler.getRootElement();
    }

    class ParserHandler extends DefaultHandler {
        private Element root;
        private Element currentElement;
        List<Element> stackElements;

        public ParserHandler() {
            this.stackElements = new LinkedList<Element>();
        }

        public Element getRootElement() {
            return root;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {

        }

        @Override
        public void endElement(String uri, String localName, String qName) {

        }

        @Override
        public void characters (char ch[], int start, int length) {

        }
    }
}
