/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the
 * License.
 */
package org.amplafi.flow.json.renderers;

import org.amplafi.flow.json.IJsonWriter;
import org.amplafi.flow.json.JsonRenderer;
import org.apache.commons.lang.ObjectUtils;

public class BooleanJsonRenderer implements JsonRenderer<Boolean> {

    public static final BooleanJsonRenderer INSTANCE = new BooleanJsonRenderer();
    public IJsonWriter toJson(IJsonWriter jsonWriter, Boolean o) {
        return jsonWriter.append(ObjectUtils.toString(o));
    }
    @SuppressWarnings("unchecked")
    public <K> K fromJson(Class<K> clazz, Object value, Object... parameters) {
        if ( value instanceof Boolean ){
            return (K)value;
        } else if ( value instanceof CharSequence) {
            return (K) Boolean.valueOf(((CharSequence)value).toString());
        }
        return (K) (clazz == null || clazz == Boolean.class?null:Boolean.FALSE);
    }

    public Class<Boolean> getClassToRender() {
        return Boolean.class;
    }

}
