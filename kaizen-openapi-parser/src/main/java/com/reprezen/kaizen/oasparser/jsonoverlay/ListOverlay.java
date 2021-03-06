/*******************************************************************************
 *  Copyright (c) 2017 ModelSolv, Inc. and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *     ModelSolv, Inc. - initial API and implementation and/or initial documentation
 *******************************************************************************/
package com.reprezen.kaizen.oasparser.jsonoverlay;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.reprezen.kaizen.oasparser.jsonoverlay.SerializationOptions.Option;

public class ListOverlay<V, OV extends JsonOverlay<V>> extends JsonOverlay<Collection<V>> {

	private OverlayFactory<V, OV> itemFactory;
	private List<IJsonOverlay<V>> overlays = Lists.newLinkedList();

	public ListOverlay(Collection<V> value, JsonOverlay<?> parent, OverlayFactory<V, OV> itemFactory,
			ReferenceRegistry refReg) {
		super(value, parent, refReg);
		this.itemFactory = itemFactory;
		fillWithValues();
	}

	public ListOverlay(JsonNode json, JsonOverlay<?> parent, OverlayFactory<V, OV> itemFactory,
			ReferenceRegistry refReg) {
		super(json, parent, refReg);
		this.itemFactory = itemFactory;
		fillWithJson();
	}

	private void fillWithValues() {
		overlays.clear();
		if (value != null) {
			for (V item : value) {
				overlays.add(new ChildOverlay<V, OV>(null, item, this, itemFactory, refReg));
			}
		}
	}

	private void fillWithJson() {
		value.clear();
		overlays.clear();
		for (JsonNode itemJson : iterable(json.elements())) {
			ChildOverlay<V, OV> overlay = new ChildOverlay<>(null, itemJson, this, itemFactory, refReg);
			overlays.add(overlay);
			value.add(overlay.get(false));
		}
	}

	public Collection<V> get(boolean elaborate) {
		return value;
	}

	@Override
	protected Collection<V> fromJson(JsonNode json) {
		return Lists.newArrayList();
	}

	@Override
	public IJsonOverlay<?> _find(JsonPointer path) {
		int index = path.getMatchingIndex();
		return overlays.size() > index ? overlays.get(index).find(path.tail()) : null;
	}

	@Override
	public JsonNode toJson(SerializationOptions options) {
		ArrayNode array = jsonArray();
		for (IJsonOverlay<V> overlay : overlays) {
			array.add(overlay.toJson(options.plus(Option.KEEP_ONE_EMPTY)));
		}
		return array.size() > 0 || options.isKeepThisEmpty() ? array : jsonMissing();
	}

	public V get(int index) {
		IJsonOverlay<V> overlay = overlays.get(index);
		return overlay != null ? overlay.get() : null;
	}

	public IJsonOverlay<V> getOverlay(int index) {
		return overlays.get(index);
	}

	public void set(int index, V value) {
		overlays.set(index, itemFactory.create(value, this, refReg));
	}

	public void add(V value) {
		overlays.add(itemFactory.create(value, this, refReg));
	}

	public void insert(int index, V value) {
		overlays.add(index, itemFactory.create(value, this, refReg));
	}

	public void remove(int index) {
		overlays.remove(index);
	}

	public int size() {
		return overlays.size();
	}

	public boolean isReference(int index) {
		@SuppressWarnings("unchecked")
		ChildOverlay<V, OV> childOverlay = (ChildOverlay<V, OV>) overlays.get(index);
		return childOverlay.isReference();
	}

	public Reference getReference(int index) {
		@SuppressWarnings("unchecked")
		ChildOverlay<V, OV> childOverlay = (ChildOverlay<V, OV>) overlays.get(index);
		return childOverlay.getReference();
	}

	public static <V, OV extends JsonOverlay<V>> OverlayFactory<Collection<V>, ListOverlay<V, OV>> getFactory(
			OverlayFactory<V, OV> itemFactory) {
		return new ListOverlayFactory<V, OV>(itemFactory);
	}

	private static class ListOverlayFactory<V, OV extends JsonOverlay<V>>
			extends OverlayFactory<Collection<V>, ListOverlay<V, OV>> {

		private OverlayFactory<V, OV> itemFactory;

		public ListOverlayFactory(OverlayFactory<V, OV> itemFactory) {
			this.itemFactory = itemFactory;
		}

		@Override
		protected Class<? super ListOverlay<V, OV>> getOverlayClass() {
			return ListOverlay.class;
		}

		@Override
		public ListOverlay<V, OV> _create(Collection<V> value, JsonOverlay<?> parent, ReferenceRegistry refReg) {
			return new ListOverlay<V, OV>(value, parent, itemFactory, refReg);
		}

		@Override
		public ListOverlay<V, OV> _create(JsonNode json, JsonOverlay<?> parent, ReferenceRegistry refReg) {
			return new ListOverlay<V, OV>(json, parent, itemFactory, refReg);
		}
	}
}
