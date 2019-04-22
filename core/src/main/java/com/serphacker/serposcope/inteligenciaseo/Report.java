package com.serphacker.serposcope.inteligenciaseo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Iterator;

public class Report {
    private final int id;
    private final int groupId;
    private final String name;
    private final String iframe;

    public Report(int id, int groupId, String name, String iframe) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.iframe = iframe;
    }

    public Report(int groupId, String name, String iframe) {
        this.id = -1;
        this.groupId = groupId;
        this.name = name;
        this.iframe = iframe;
    }

    public int getGroupId() {
        return groupId;
    }

    public String getName() {
        return name;
    }

    public String getIframe() {
        return iframe;
    }

    public int getId() {
        return id;
    }

    public String getIframeAttributes() {

        StringBuilder builder = new StringBuilder();
        Document document = Jsoup.parse("<!DOCTYPE html><body>" + iframe + "</body>");
        Elements elements = document.getElementsByTag("iframe");
        if (elements.size() == 0) {
            return "{}";
        }

        Element element = elements.first();
        Attributes allAttributes = element.attributes();
        builder.append('{');
        for (Iterator<Attribute> it = allAttributes.iterator(); it.hasNext(); ) {
            Attribute item = it.next();

            builder.append('"');
            builder.append(item.getKey());
            builder.append('"');
            builder.append(':');
            builder.append('"');
            builder.append(item.getValue());
            builder.append('"');

            if (it.hasNext()) {
                builder.append(',');
            }
        }
        builder.append('}');
        return builder.toString();
    }
}
