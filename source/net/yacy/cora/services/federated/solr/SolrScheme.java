/**
 *  SolrScheme
 *  Copyright 2011 by Michael Peter Christen
 *  First released 14.04.2011 at http://yacy.net
 *
 *  $LastChangedDate: 2011-04-14 22:05:04 +0200 (Do, 14 Apr 2011) $
 *  $LastChangedRevision: 7654 $
 *  $LastChangedBy: orbiter $
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.cora.services.federated.solr;


import java.io.File;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import net.yacy.cora.document.MultiProtocolURI;
import net.yacy.cora.document.UTF8;
import net.yacy.cora.protocol.Domains;
import net.yacy.cora.protocol.HeaderFramework;
import net.yacy.cora.protocol.ResponseHeader;
import net.yacy.cora.storage.ConfigurationSet;
import net.yacy.document.Document;
import net.yacy.document.parser.html.ContentScraper;
import net.yacy.document.parser.html.ImageEntry;
import net.yacy.kelondro.data.meta.DigestURI;

import org.apache.solr.common.SolrInputDocument;

public class SolrScheme extends ConfigurationSet {

    /**
     * initialize with an empty ConfigurationSet which will cause that all the index
     * attributes are used
     */
    public SolrScheme() {
        super();
    }

    /**
     * initialize the scheme with a given configuration file
     * the configuration file simply contains a list of lines with keywords
     * @param configurationFile
     */
    public SolrScheme(final File configurationFile) {
        super(configurationFile);
    }

    private void addSolr(final SolrInputDocument solrdoc, final String key, final String value) {
        if (isEmpty() || contains(key)) solrdoc.setField(key, value);
    }

    private void addSolr(final SolrInputDocument solrdoc, final String key, final Date value) {
        if (isEmpty() || contains(key)) solrdoc.setField(key, value);
    }

    private void addSolr(final SolrInputDocument solrdoc, final String key, final int value) {
        if (isEmpty() || contains(key)) solrdoc.setField(key, value);
    }

    private void addSolr(final SolrInputDocument solrdoc, final String key, final String[] value) {
        if (isEmpty() || contains(key)) solrdoc.setField(key, value);
    }

    private void addSolr(final SolrInputDocument solrdoc, final String key, final float value) {
        if (isEmpty() || contains(key)) solrdoc.setField(key, value);
    }

    private void addSolr(final SolrInputDocument solrdoc, final String key, final boolean value) {
        if (isEmpty() || contains(key)) solrdoc.setField(key, value);
    }

    private void addSolr(final SolrInputDocument solrdoc, final String key, final String value, final float boost) {
        if (isEmpty() || contains(key)) solrdoc.setField(key, value, boost);
    }

    public SolrInputDocument yacy2solr(final String id, final ResponseHeader header, final Document yacydoc) {
        // we user the SolrCell design as index scheme
        final SolrInputDocument solrdoc = new SolrInputDocument();
        final DigestURI digestURI = new DigestURI(yacydoc.dc_source());
        addSolr(solrdoc, "failreason_t", ""); // overwrite a possible fail reason (in case that there was a fail reason before)
        addSolr(solrdoc, "id", id);
        addSolr(solrdoc, "sku", digestURI.toNormalform(true, false), 3.0f);
        final InetAddress address = Domains.dnsResolve(digestURI.getHost());
        if (address != null) addSolr(solrdoc, "ip_s", address.getHostAddress());
        if (digestURI.getHost() != null) addSolr(solrdoc, "host_s", digestURI.getHost());
        addSolr(solrdoc, "title", yacydoc.dc_title());
        addSolr(solrdoc, "author", yacydoc.dc_creator());
        addSolr(solrdoc, "description", yacydoc.dc_description());
        addSolr(solrdoc, "content_type", yacydoc.dc_format());
        addSolr(solrdoc, "last_modified", header.lastModified());
        addSolr(solrdoc, "keywords", yacydoc.dc_subject(' '));
        final String content = UTF8.String(yacydoc.getTextBytes());
        addSolr(solrdoc, "text_t", content);
        if (contains("wordcount_i")) {
            final int contentwc = content.split(" ").length;
            addSolr(solrdoc, "wordcount_i", contentwc);
        }

        // path elements of link
        final String path = digestURI.getPath();
        if (path != null && contains("attr_paths")) {
            final String[] paths = path.split("/");
            if (paths.length > 0) addSolr(solrdoc, "attr_paths", paths);
        }

        // list all links
        final Map<MultiProtocolURI, Properties> alllinks = yacydoc.getAnchors();
        int c = 0;
        addSolr(solrdoc, "inboundlinkscount_i", yacydoc.inboundLinkCount());
        if (contains("attr_inboundlinks")) {
            final String[] inboundlinks = new String[yacydoc.inboundLinkCount()];
            for (final MultiProtocolURI url: yacydoc.inboundLinks()) {
                final Properties p = alllinks.get(url);
                final String name = p.getProperty("name", "");
                final String rel = p.getProperty("rel", "");
                inboundlinks[c++] =
                    "<a href=\"" + url.toNormalform(false, false) + "\"" +
                    ((rel.toLowerCase().equals("nofollow")) ? " rel=\"nofollow\"" : "") +
                    ">" +
                    ((name.length() > 0) ? name : "") + "</a>";
            }
            addSolr(solrdoc, "attr_inboundlinks", inboundlinks);
        }
        c = 0;
        final String[] outboundlinks = new String[yacydoc.outboundLinkCount()];
        if (contains("attr_outboundlinks")) {
            addSolr(solrdoc, "outboundlinkscount_i", outboundlinks.length);
            for (final MultiProtocolURI url: yacydoc.outboundLinks()) {
                final Properties p = alllinks.get(url);
                final String name = p.getProperty("name", "");
                final String rel = p.getProperty("rel", "");
                outboundlinks[c++] =
                    "<a href=\"" + url.toNormalform(false, false) + "\"" +
                    ((rel.toLowerCase().equals("nofollow")) ? " rel=\"nofollow\"" : "") +
                    ">" +
                    ((name.length() > 0) ? name : "") + "</a>";
            }
            addSolr(solrdoc, "attr_outboundlinks", outboundlinks);
        }
        // charset
        addSolr(solrdoc, "charset_s", yacydoc.getCharset());

        // coordinates
        if (yacydoc.lat() != 0.0f && yacydoc.lon() != 0.0f) {
            addSolr(solrdoc, "lon_coordinate", yacydoc.lon());
            addSolr(solrdoc, "lat_coordinate", yacydoc.lat());
        }
        addSolr(solrdoc, "httpstatus_i", 200);
        final Object parser = yacydoc.getParserObject();
        if (parser instanceof ContentScraper) {
            final ContentScraper html = (ContentScraper) parser;

            // header tags
            int h = 0;
            int f = 1;
            for (int i = 1; i <= 6; i++) {
                final String[] hs = html.getHeadlines(i);
                h = h | (hs.length > 0 ? f : 0);
                f = f * 2;
                addSolr(solrdoc, "attr_h" + i, hs);
            }
            addSolr(solrdoc, "htags_i", h);

            // meta tags
            final Map<String, String> metas = html.getMetas();
            final String robots = metas.get("robots");
            if (robots != null) addSolr(solrdoc, "metarobots_t", robots);
            final String generator = metas.get("generator");
            if (generator != null) addSolr(solrdoc, "metagenerator_t", generator);

            // bold, italic
            final String[] bold = html.getBold();
            addSolr(solrdoc, "boldcount_i", bold.length);
            if (bold.length > 0) {
                addSolr(solrdoc, "attr_bold", bold);
                if (contains("attr_boldcount")) {
                    addSolr(solrdoc, "attr_boldcount", html.getBoldCount(bold));
                }
            }
            final String[] italic = html.getItalic();
            addSolr(solrdoc, "italiccount_i", italic.length);
            if (italic.length > 0) {
                addSolr(solrdoc, "attr_italic", italic);
                if (contains("attr_italiccount")) {
                    addSolr(solrdoc, "attr_italiccount", html.getItalicCount(italic));
                }
            }
            final String[] li = html.getLi();
            addSolr(solrdoc, "licount_i", li.length);
            if (li.length > 0) addSolr(solrdoc, "attr_li", li);

            // images
            if (contains("attr_images")) {
                final Collection<ImageEntry> imagesc = html.getImages().values();
                final String[] images = new String[imagesc.size()];
                c = 0;
                for (final ImageEntry ie: imagesc) images[c++] = ie.toString();
                addSolr(solrdoc, "imagescount_i", images.length);
                if (images.length > 0) addSolr(solrdoc, "attr_images", images);
            }

            // style sheets
            if (contains("attr_css")) {
                final Map<MultiProtocolURI, String> csss = html.getCSS();
                final String[] css = new String[csss.size()];
                c = 0;
                for (final Map.Entry<MultiProtocolURI, String> entry: csss.entrySet()) {
                    css[c++] =
                        "<link rel=\"stylesheet\" type=\"text/css\" media=\"" + entry.getValue() + "\"" +
                        " href=\""+ entry.getKey().toNormalform(false, false, false, false) + "\" />";
                }
                addSolr(solrdoc, "csscount_i", css.length);
                if (css.length > 0) addSolr(solrdoc, "attr_css", css);
            }

            // Scripts
            if (contains("attr_scripts")) {
                final Set<MultiProtocolURI> scriptss = html.getScript();
                final String[] scripts = new String[scriptss.size()];
                c = 0;
                for (final MultiProtocolURI url: scriptss) {
                    scripts[c++] = url.toNormalform(false, false, false, false);
                }
                addSolr(solrdoc, "scriptscount_i", scripts.length);
                if (scripts.length > 0) addSolr(solrdoc, "attr_scripts", scripts);
            }

            // Frames
            if (contains("attr_frames")) {
                final Set<MultiProtocolURI> framess = html.getFrames();
                final String[] frames = new String[framess.size()];
                c = 0;
                for (final MultiProtocolURI entry: framess) {
                    frames[c++] = entry.toNormalform(false, false, false, false);
                }
                addSolr(solrdoc, "framesscount_i", frames.length);
                if (frames.length > 0) addSolr(solrdoc, "attr_frames", frames);
            }

            // IFrames
            if (contains("attr_iframes")) {
                final Set<MultiProtocolURI> iframess = html.getIFrames();
                final String[] iframes = new String[iframess.size()];
                c = 0;
                for (final MultiProtocolURI entry: iframess) {
                    iframes[c++] = entry.toNormalform(false, false, false, false);
                }
                addSolr(solrdoc, "iframesscount_i", iframes.length);
                if (iframes.length > 0) addSolr(solrdoc, "attr_iframes", iframes);
            }

            // flash embedded
            addSolr(solrdoc, "flash_b", html.containsFlash());

            // generic evaluation pattern
            for (final String model: html.getEvaluationModelNames()) {
                if (contains("attr_" + model)) {
                    final String[] scorenames = html.getEvaluationModelScoreNames(model);
                    if (scorenames.length > 0) {
                        addSolr(solrdoc, "attr_" + model, scorenames);
                        addSolr(solrdoc, "attr_" + model + "count", html.getEvaluationModelScoreCounts(model, scorenames));
                    }
                }
            }

            // response time
            addSolr(solrdoc, "responsetime_i", header.get(HeaderFramework.RESPONSE_TIME_MILLIS, "0"));
        }
        return solrdoc;
    }


    /*
     * standard solr scheme

   <field name="name" type="textgen" indexed="true" stored="true"/>
   <field name="cat" type="string" indexed="true" stored="true" multiValued="true"/>
   <field name="features" type="text" indexed="true" stored="true" multiValued="true"/>
   <field name="includes" type="text" indexed="true" stored="true" termVectors="true" termPositions="true" termOffsets="true" />

   <field name="weight" type="float" indexed="true" stored="true"/>
   <field name="price"  type="float" indexed="true" stored="true"/>
   <field name="popularity" type="int" indexed="true" stored="true" />

   <!-- Common metadata fields, named specifically to match up with
     SolrCell metadata when parsing rich documents such as Word, PDF.
     Some fields are multiValued only because Tika currently may return
     multiple values for them.
   -->
   <field name="title" type="text" indexed="true" stored="true" multiValued="true"/>
   <field name="subject" type="text" indexed="true" stored="true"/>
   <field name="description" type="text" indexed="true" stored="true"/>
   <field name="comments" type="text" indexed="true" stored="true"/>
   <field name="author" type="textgen" indexed="true" stored="true"/>
   <field name="keywords" type="textgen" indexed="true" stored="true"/>
   <field name="category" type="textgen" indexed="true" stored="true"/>
   <field name="content_type" type="string" indexed="true" stored="true" multiValued="true"/>
   <field name="last_modified" type="date" indexed="true" stored="true"/>
   <field name="links" type="string" indexed="true" stored="true" multiValued="true"/>

     */
}