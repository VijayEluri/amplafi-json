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
package org.amplafi.flow.translator;

import java.util.ArrayList;
import java.util.List;

import org.amplafi.flow.FlowPropertyDefinition;
import org.amplafi.flow.DataClassDefinition;
import org.amplafi.flow.flowproperty.FlowPropertyProvider;
import org.amplafi.flow.json.renderers.IterableJsonOutputRenderer;


public class ListFlowTranslator<T> extends FlowCollectionTranslator<List<? extends T>, T> {
    public ListFlowTranslator() {
        super(new IterableJsonOutputRenderer<List<? extends T>>());
    }
    @Override
    public List<? extends T> deserialize(FlowPropertyProvider flowPropertyProvider, FlowPropertyDefinition flowPropertyDefinition, DataClassDefinition dataClassDefinition, Object serializedObject) {
        if ( serializedObject != null ) {
            List<T> list = new ArrayList<T>();
            super.deserialize(flowPropertyProvider, flowPropertyDefinition, dataClassDefinition, list, serializedObject);
            return list;
        } else {
            return null;
        }
    }

    @Override
    public Class<?> getTranslatedClass() {
        return List.class;
    }

    @Override
    public List<? extends T> getDefaultObject(FlowPropertyProvider flowPropertyProvider) {
        return new ArrayList<T>();
    }

}
