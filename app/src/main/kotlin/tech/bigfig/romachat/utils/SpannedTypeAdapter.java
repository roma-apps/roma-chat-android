/* Copyright 2017 Andrew Dawson
 *
 * This file is a part of Roma.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of the
 * License, or (at your option) any later version.
 *
 * Roma is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Roma; if not,
 * see <http://www.gnu.org/licenses>. */

package tech.bigfig.romachat.utils;

import android.text.Spanned;
import android.text.SpannedString;
import com.google.gson.*;

import java.lang.reflect.Type;

public class SpannedTypeAdapter implements JsonDeserializer<Spanned>, JsonSerializer<Spanned> {
    @Override
    public Spanned deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        String string = json.getAsString();
        if (string != null) {
            return HtmlUtils.fromHtml(string);
        } else {
            return new SpannedString("");
        }
    }

    @Override
    public JsonElement serialize(Spanned src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(HtmlUtils.toHtml(src));
    }
}
