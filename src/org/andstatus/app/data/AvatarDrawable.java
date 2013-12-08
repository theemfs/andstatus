/**
 * Copyright (C) 2013 yvolk (Yuri Volkov), http://yurivolkov.com
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

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;

import org.andstatus.app.CommandData;
import org.andstatus.app.MyContextHolder;
import org.andstatus.app.MyServiceManager;
import org.andstatus.app.R;
import org.andstatus.app.MyService.CommandEnum;
import org.andstatus.app.util.MyLog;

import java.io.File;
import java.io.IOException;

public class AvatarDrawable {
    private long userId;
    private File file = null;
    public static final int AVATAR_SIZE_DIP = 48;
    
    private static Drawable defaultAvatar = loadDefaultAvatar();
    
    public AvatarDrawable(long userIdIn, String fileName) {
        userId = userIdIn;
        if (!TextUtils.isEmpty(fileName)) {
            file = new File(MyPreferences.getDataFilesDir("avatars", null), fileName);
        }
    }
    
    private static Drawable loadDefaultAvatar() {
        Drawable avatar = null;
        MyLog.v(AvatarDrawable.class, "Loading default avatar");
        Context context = MyContextHolder.get().context();
        if (context != null) {
            avatar = context.getResources().getDrawable(R.drawable.avatar_default);
        }
        return avatar;
    }

    public Drawable getDrawable() {
        if (isLoaded()) {
            try {
                String pathName = file.getCanonicalPath();
                return Drawable.createFromPath(pathName);
            } catch (IOException e) {
                MyLog.e(this, "File for userId=" + userId, e);
            }
        }
        MyServiceManager.sendCommand(new CommandData(CommandEnum.FETCH_AVATAR, null, userId));
        return defaultAvatar;
    }

    public boolean isLoaded() {
        return file != null && file.exists();
    }
    
    public File getFile() {
        return file;
    }
}