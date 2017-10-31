/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencron.common.util;

import java.util.*;

/**
 * Static utility methods pertaining to {@link List} instances.
 *
 * jupiter
 * org.jupiter.common.util
 *
 * @author jiachun.fjc
 */
public final class Lists {

    /**
     * Creates a mutable, empty {@code ArrayList} instance.
     */
    public static <E> ArrayList<E> newArrayList() {
        return new ArrayList<>();
    }

    /**
     * Creates a mutable {@code ArrayList} instance containing the given elements.
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> newArrayList(E... elements) {
        AssertUtils.checkNotNull(elements);
        // Avoid integer overflow when a large array is passed in
        int capacity = computeArrayListCapacity(elements.length);
        ArrayList<E> list = new ArrayList<>(capacity);
        Collections.addAll(list, elements);
        return list;
    }

    /**
     * Creates a mutable {@code ArrayList} instance containing the given elements.
     */
    @SuppressWarnings("unchecked")
    public static <E> ArrayList<E> newArrayList(Iterable<? extends E> elements) {
        AssertUtils.checkNotNull(elements);
        return elements instanceof Collection
                ? new ArrayList((Collection<E>) elements)
                : newArrayList(elements.iterator());
    }

    /**
     * Creates a mutable {@code ArrayList} instance containing the given elements.
     */
    public static <E> ArrayList<E> newArrayList(Iterator<? extends E> elements) {
        ArrayList<E> list = newArrayList();
        while (elements.hasNext()) {
            list.add(elements.next());
        }
        return list;
    }

    /**
     * Creates an {@code ArrayList} instance backed by an array of the exact size specified;
     * equivalent to {@link ArrayList#ArrayList(int)}.
     */
    public static <E> ArrayList<E> newArrayListWithCapacity(int initialArraySize) {
        AssertUtils.checkArgument(initialArraySize >= 0);
        return new ArrayList<>(initialArraySize);
    }

    abstract static class TransformedIterator<F, T> implements Iterator<T> {
        final Iterator<? extends F> backingIterator;

        TransformedIterator(Iterator<? extends F> backingIterator) {
            this.backingIterator = AssertUtils.checkNotNull(backingIterator);
        }

        abstract T transform(F from);

        @Override
        public final boolean hasNext() {
            return backingIterator.hasNext();
        }

        @Override
        public final T next() {
            return transform(backingIterator.next());
        }

        @Override
        public final void remove() {
            backingIterator.remove();
        }
    }

    abstract static class TransformedListIterator<F, T> extends TransformedIterator<F, T> implements ListIterator<T> {

        TransformedListIterator(ListIterator<? extends F> backingIterator) {
            super(backingIterator);
        }

        @SuppressWarnings("unchecked")
        private ListIterator<? extends F> backingIterator() {
            return (ListIterator<? extends F>) backingIterator;
        }

        @Override
        public final boolean hasPrevious() {
            return backingIterator().hasPrevious();
        }

        @Override
        public final T previous() {
            return transform(backingIterator().previous());
        }

        @Override
        public final int nextIndex() {
            return backingIterator().nextIndex();
        }

        @Override
        public final int previousIndex() {
            return backingIterator().previousIndex();
        }

        @Override
        public void set(T element) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(T element) {
            throw new UnsupportedOperationException();
        }
    }

    static int computeArrayListCapacity(int arraySize) {
        AssertUtils.checkArgument(arraySize >= 0);
        return Ints.saturatedCast(5L + arraySize + (arraySize / 10));
    }

    private Lists() {}
}