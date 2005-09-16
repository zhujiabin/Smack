/**
 * $RCSfile: ,v $
 * $Revision: 1.0 $
 * $Date: 2005/05/25 04:20:03 $
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is
 subject to license terms.
 */

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.ReportedData;
import org.xmlpull.v1.XmlPullParser;

/**
 * Implements the protocol currently used to search information repositories on the Jabber network. To date, the jabber:iq:search protocol
 * has been used mainly to search for people who have registered with user directories (e.g., the "Jabber User Directory" hosted at users.jabber.org).
 * However, the jabber:iq:search protocol is not limited to user directories, and could be used to search other Jabber information repositories
 * (such as chatroom directories) or even to provide a Jabber interface to conventional search engines.
 * <p/>
 * The basic functionality is to query an information repository regarding the possible search fields, to send a search query, and to receive search results.
 *
 * @author Derek DeMoro
 */
public class UserSearch extends IQ {

    public UserSearch() {

    }


    public String getChildElementXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<query xmlns=\"jabber:iq:search\">");
        buf.append(getExtensionsXML());
        buf.append("</query>");
        return buf.toString();
    }


    /**
     * Returns the form for all search fields supported by the search service.
     *
     * @param con           the current XMPPConnection.
     * @param searchService the search service to use. (ex. search.jivesoftware.com)
     * @throws org.jivesoftware.smack.XMPPException
     *          thrown if a server error has occured.
     */
    public Form getSearchForm(XMPPConnection con, String searchService) throws XMPPException {
        UserSearch search = new UserSearch();
        search.setType(IQ.Type.GET);
        search.setTo(searchService);

        PacketCollector collector = con.createPacketCollector(new PacketIDFilter(search.getPacketID()));
        con.sendPacket(search);

        IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return Form.getFormFrom(response);
    }

    /**
     * Sends the filled out answer form to be sent and queried by the search service.
     *
     * @param con           the current XMPPConnection.
     * @param searchForm    the <code>Form</code> to send for querying.
     * @param searchService the search service to use. (ex. search.jivesoftware.com)
     * @return ReportedData the data found from the query.
     * @throws org.jivesoftware.smack.XMPPException
     *          thrown if a server error has occured.
     */
    public ReportedData sendSearchForm(XMPPConnection con, Form searchForm, String searchService) throws XMPPException {
        UserSearch search = new UserSearch();
        search.setType(IQ.Type.SET);
        search.setTo(searchService);
        search.addExtension(searchForm.getDataFormToSend());

        PacketCollector collector = con.createPacketCollector(new PacketIDFilter(search.getPacketID()));

        con.sendPacket(search);

        IQ response = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());

        // Cancel the collector.
        collector.cancel();
        if (response == null) {
            throw new XMPPException("No response from server on status set.");
        }
        if (response.getError() != null) {
            throw new XMPPException(response.getError());
        }
        return ReportedData.getReportedDataFrom(response);
    }

    /**
     * Internal Search service Provider.
     */
    public static class Provider implements IQProvider {

        public Provider() {
            super();
        }

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            UserSearch search = new UserSearch();
            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if (eventType == XmlPullParser.START_TAG) {
                    // Otherwise, it must be a packet extension.
                    search.addExtension(PacketParserUtils.parsePacketExtension(parser.getName(),
                            parser.getNamespace(), parser));

                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("query")) {
                        done = true;
                    }
                }
            }

            return search;
        }
    }

}