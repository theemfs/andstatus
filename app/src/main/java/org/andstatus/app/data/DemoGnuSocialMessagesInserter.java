/*
 * Copyright (C) 2014 yvolk (Yuri Volkov), http://yurivolkov.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.andstatus.app.data;

import android.support.annotation.Nullable;

import org.andstatus.app.context.DemoData;
import org.andstatus.app.context.MyContextHolder;
import org.andstatus.app.database.MsgTable;
import org.andstatus.app.net.social.MbActivity;
import org.andstatus.app.net.social.MbActivityType;
import org.andstatus.app.net.social.MbMessage;
import org.andstatus.app.net.social.MbUser;
import org.andstatus.app.origin.Origin;
import org.andstatus.app.service.CommandData;
import org.andstatus.app.service.CommandEnum;
import org.andstatus.app.service.CommandExecutionContext;
import org.andstatus.app.util.MyLog;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DemoGnuSocialMessagesInserter {
    private static AtomicInteger iterationCounter = new AtomicInteger(0);
    private int iteration = 0;
    private String conversationOid = "";

    private MbUser accountUser;
    private Origin origin;

    public void insertData() {
        mySetup();
        addConversation();
    }
    
    private void mySetup() {
        iteration = iterationCounter.incrementAndGet();
        conversationOid = Long.toString(MyLog.uniqueCurrentTimeMS());
        origin = MyContextHolder.get().persistentOrigins().fromName(DemoData.GNUSOCIAL_TEST_ORIGIN_NAME);
        assertTrue(DemoData.GNUSOCIAL_TEST_ORIGIN_NAME + " exists", origin.isValid());
        accountUser = MyContextHolder.get().persistentAccounts().fromAccountName(DemoData.GNUSOCIAL_TEST_ACCOUNT_NAME)
                .toPartialUser();
    }
    
    private void addConversation() {
        MbUser author1 = userFromOidAndAvatar("1",
                "https://raw.github.com/andstatus/andstatus/master/app/src/main/res/drawable/splash_logo.png");
        MbUser author2 = userFromOidAndAvatar("2",
                "http://png.findicons.com/files/icons/1780/black_and_orange/300/android_orange.png");
        MbUser author3 = userFromOidAndAvatar("3",
                "http://www.large-icons.com/stock-icons/free-large-android/48x48/happy-robot.gif");
        MbUser author4 = userFromOidAndAvatar("4", "");

        MbMessage minus1 = buildMessage(author2, "Older one message", null, null);
        MbMessage selected = buildMessage(author1, "Selected message", minus1,
                iteration == 1 ? DemoData.CONVERSATION_ENTRY_MESSAGE_OID : null);
        MbMessage reply1 = buildMessage(author3, "Reply 1 to selected", selected, null);
        MbMessage reply2 = buildMessage(author2, "Reply 2 to selected is public", selected, null);
        addPublicMessage(reply2, true);
        MbMessage reply3 = buildMessage(author1, "Reply 3 to selected by the same author", selected, null);
        addMessage(selected);
        addMessage(reply3);
        addMessage(reply1);
        addMessage(reply2);
        MbMessage reply4 = buildMessage(author4, "Reply 4 to Reply 1, " + DemoData.PUBLIC_MESSAGE_TEXT + " other author", reply1, null);
        addMessage(reply4);
        addPublicMessage(reply4, false);
        addMessage(buildMessage(author2, "Reply 5 to Reply 4", reply4, null));
        addMessage(buildMessage(author3, "Reply 6 to Reply 4 - the second", reply4, null));

        MbMessage reply7 = buildMessage(author1, "Reply 7 to Reply 2 is about " 
        + DemoData.PUBLIC_MESSAGE_TEXT + " and something else", reply2, null);
        addPublicMessage(reply7, true);
        
        MbMessage reply8 = buildMessage(author4, "<b>Reply 8</b> to Reply 7", reply7, null);
        MbMessage reply9 = buildMessage(author2, "Reply 9 to Reply 7", reply7, null);
        addMessage(reply9);
        MbMessage reply10 = buildMessage(author3, "Reply 10 to Reply 8", reply8, null);
        addMessage(reply10);
        MbMessage reply11 = buildMessage(author2, "Reply 11 to Reply 7 with " + DemoData.GLOBAL_PUBLIC_MESSAGE_TEXT + " text", reply7, null);
        addPublicMessage(reply11, true);

        MbMessage reply12 = buildMessage(author2, "Reply 12 to Reply 7 reblogged by author1", reply7, null);
        MbActivity activity = MbActivity.from(accountUser, MbActivityType.ANNOUNCE);
        activity.setActor(author1);
        activity.setMessage(reply12);
        DemoMessageInserter.onActivityS(activity);
    }
    
    private void addPublicMessage(MbMessage message, boolean isPublic) {
        message.setPublic(isPublic);
        long id = addMessage(message);
        long storedPublic = MyQuery.msgIdToLongColumnValue(MsgTable.PUBLIC, id);
        assertTrue("Message is " + (isPublic ? "public" : "private" )+ ": " + message.getBody(), (isPublic == ( storedPublic != 0)));
    }

    private MbUser userFromOidAndAvatar(String userOid, @Nullable String avatarUrl) {
        String userName = "user" + userOid;
        MbUser mbUser = MbUser.fromOriginAndUserOid(origin.getId(), userOid);
        mbUser.setUserName(userName);
        if (avatarUrl != null) {
            mbUser.avatarUrl = avatarUrl;
        }
        mbUser.setProfileUrl(origin.getUrl());
        return mbUser;
    }
    
    private MbMessage buildMessage(MbUser author, String body, MbMessage inReplyToMessage, String messageOidIn) {
        return new DemoMessageInserter(accountUser).buildMessage(author, body
                        + (inReplyToMessage != null ? " it" + iteration : ""),
                inReplyToMessage, messageOidIn, DownloadStatus.LOADED)
                .setConversationOid(conversationOid);
    }
    
    private long addMessage(MbMessage message) {
        DataUpdater di = new DataUpdater(new CommandExecutionContext(
                CommandData.newOriginCommand(CommandEnum.EMPTY, origin)));
        long messageId = di.onActivity(message.update(accountUser));
        assertTrue( "Message added " + message.oid, messageId != 0);
        assertEquals("Conversation Oid", conversationOid, MyQuery.msgIdToConversationOid(messageId));
        return messageId;
    }
}
