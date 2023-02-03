package com.teamwork.animalshelter.parser;

import com.teamwork.animalshelter.exception.ErrorElementXmlFile;
import com.teamwork.animalshelter.exception.WrongFormatXmlFile;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Класс служит для парсинга произвольных файлов XML. Позволяет создать объектную модель файла XML.
 */
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

    /**
     * Запускает парсинг файла XML.
     * @param filePath путь к файлу
     * @return {@code Element} корневой элемент спарсенного файла XML
     * @see Element
     */
    public Element parse(File file) {
        try {
            parser.parse(file, handler);
        } catch (SAXException e) {
            throw new WrongFormatXmlFile(file.getPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return handler.getRootElement();
    }

    /**
     * Вспомогательный класс для класса {@link ParserXML}
     * <br> Определяет обработчики событийной модели из библиотеки  {@code org.xml.sax},
     * c помощью которых строится объектная модель файла XML.
     * @see ParserXML
     * @see Element
     */
    class ParserHandler extends DefaultHandler {
        private Element root;
        private Element currentElement;
        LinkedList<Element> stackElements;

        public ParserHandler() {
            this.stackElements = new LinkedList<Element>();
        }

        public Element getRootElement() {
            return root;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            Element elem = new Element(qName, currentElement);
            for (int i = 0; i < attributes.getLength(); i++) {
                elem.addAttribute(attributes.getQName(i), attributes.getValue(i));
            }
            if (currentElement != null) {
                stackElements.push(currentElement);
                currentElement.addChild(elem);
            }
            currentElement = elem;
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            if (currentElement == null) {
                throw new ErrorElementXmlFile(qName, "");
            }
            if (!qName.equals(currentElement.getName())) {
                throw new ErrorElementXmlFile(qName, currentElement.getName());
            }
            try {
                currentElement = stackElements.pop();
            } catch (NoSuchElementException e) {
                throw new ErrorElementXmlFile(currentElement.getName());
            }
        }

        @Override
        public void characters (char ch[], int start, int length) {
            if (currentElement == null) {
                throw new ErrorElementXmlFile(ch, start, length);
            }
            currentElement.setText(String.valueOf(ch, start, length));
        }
    }
}
