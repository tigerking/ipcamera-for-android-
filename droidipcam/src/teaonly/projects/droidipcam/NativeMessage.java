package teaonly.projects.droidipcam;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import android.util.Log;

public class NativeMessage {
	public String v0 = "";
	public String v1 = "";
	public String v2 = "";
	public String v3 = "";
    public String v4 = "";
    public String v5 = "";

	public boolean parse(String msg) {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		try {
			SAXParser parser = factory.newSAXParser();
			NativeMessageHandler handler = new NativeMessageHandler();
			InputStream is = new ByteArrayInputStream(msg.getBytes());
			parser.parse(is, handler);
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
    
		if ( v0 == null )
            return false;

        return true;
	}

	private class NativeMessageHandler extends DefaultHandler {
		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
            super.endElement(uri, localName, name);
		}

		@Override
		public void startDocument() throws SAXException {
			super.startDocument();
		}

		@Override
		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
            for (int i = 0; i < attributes.getLength(); i++) {
                if (attributes.getLocalName(i).equalsIgnoreCase("V0")) {
					v0 = attributes.getValue(i);
				} else if (attributes.getLocalName(i).equalsIgnoreCase("V1")) {
					v1 = attributes.getValue(i);
				} else if (attributes.getLocalName(i).equalsIgnoreCase("V2")) {
					v2 = attributes.getValue(i);
                } else if (attributes.getLocalName(i).equalsIgnoreCase("V3")) {
					v3 = attributes.getValue(i);
                } else if (attributes.getLocalName(i).equalsIgnoreCase("V4")) {
                    v4 = attributes.getValue(i);
                } else if (attributes.getLocalName(i).equalsIgnoreCase("V5")) {
                    v5 = attributes.getValue(i);
				}
				
			}
		}
	}
}
