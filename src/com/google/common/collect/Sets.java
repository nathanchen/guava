/*
 * Copyright (C) 2007 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2.FilteredCollection;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.annotation.Nullable;

/**
 * Static utility methods pertaining to {@link Set} instances. Also see this
 * class's counterparts {@link Lists} and {@link Maps}.
 *
 * @author Kevin Bourrillion
 * @author Jared Levy
 * @author Chris Povirk
 * @since 2 (imported from Google Collections Library)
 */
@GwtCompatible(emulated = true)
public final class Sets {
  private Sets() {}

  /**
   * Returns an immutable set instance containing the given enum elements.
   * Internally, the returned set will be backed by an {@link EnumSet}.
   *
   * <p>The iteration order of the returned set follows the enum's iteration
   * order, not the order in which the elements are provided to the method.
   *
   * @param anElement one of the elements the set should contain
   * @param otherElements the rest of the elements the set should contain
   * @return an immutable set containing those elements, minus duplicates
   */
  // http://code.google.com/p/google-web-toolkit/issues/detail?id=3028
  @GwtCompatible(serializable = true)
  public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(
      E anElement, E... otherElements) {
    return new ImmutableEnumSet<E>(EnumSet.of(anElement, otherElements));
  }

  /**
   * Returns an immutable set instance containing the given enum elements.
   * Internally, the returned set will be backed by an {@link EnumSet}.
   *
   * <p>The iteration order of the returned set follows the enum's iteration
   * order, not the order in which the elements appear in the given collection.
   *
   * @param elements the elements, all of the same {@code enum} type, that the
   *     set should contain
   * @return an immutable set containing those elements, minus duplicates
   */
  // http://code.google.com/p/google-web-toolkit/issues/detail?id=3028
  @GwtCompatible(serializable = true)
  public static <E extends Enum<E>> ImmutableSet<E> immutableEnumSet(
      Iterable<E> elements) {
    Iterator<E> iterator = elements.iterator();
    if (!iterator.hasNext()) {
      return ImmutableSet.of();
    }
    if (elements instanceof EnumSet) {
      EnumSet<E> enumSetClone = EnumSet.copyOf((EnumSet<E>) elements);
      return new ImmutableEnumSet<E>(enumSetClone);
    }
    E first = iterator.next();
    EnumSet<E> set = EnumSet.of(first);
    while (iterator.hasNext()) {
      set.add(iterator.next());
    }
    return new ImmutableEnumSet<E>(set);
  }

  /**
   * Returns a new {@code EnumSet} instance containing the given elements.
   * Unlike {@link EnumSet#copyOf(Collection)}, this method does not produce an
   * exception on an empty collection, and it may be called on any iterable, not
   * just a {@code Collection}.
   */
  public static <E extends Enum<E>> EnumSet<E> newEnumSet(Iterable<E> iterable,
      Class<E> elementType) {
    /*
     * TODO(cpovirk): noneOf() and addAll() will both throw
     * NullPointerExceptions when appropriate. However, NullPointerTester will
     * fail on this method because it passes in Class.class instead of an enum
     * type. This means that, when iterable is null but elementType is not,
     * noneOf() will throw a ClassCastException before addAll() has a chance to
     * throw a NullPointerException. NullPointerTester considers this a failure.
     * Ideally the test would be fixed, but it would require a special case for
     * Class<E> where E extends Enum. Until that happens (if ever), leave
     * checkNotNull() here. For now, contemplate the irony that checking
     * elementType, the problem argument, is harmful, while checking iterable,
     * the innocent bystander, is effective.
     */
    checkNotNull(iterable);
    EnumSet<E> set = EnumSet.noneOf(elementType);
    Iterables.addAll(set, iterable);
    return set;
  }

  // HashSet

  /**
   * Creates a <i>mutable</i>, empty {@code HashSet} instance.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableSet#of()} instead.
   *
   * <p><b>Note:</b> if {@code E} is an {@link Enum} type, use {@link
   * EnumSet#noneOf} instead.
   *
   * @return a new, empty {@code HashSet}
   */
  public static <E> HashSet<E> newHashSet() {
    return new HashSet<E>();
  }

  /**
   * Creates a <i>mutable</i> {@code HashSet} instance containing the given
   * elements in unspecified order.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are
   * non-null, use an overload of {@link ImmutableSet#of()} (for varargs) or
   * {@link ImmutableSet#copyOf(Object[])} (for an array) instead.
   *
   * <p><b>Note:</b> if {@code E} is an {@link Enum} type, use {@link
   * EnumSet#of(Enum, Enum[])} instead.
   *
   * @param elements the elements that the set should contain
   * @return a new {@code HashSet} containing those elements (minus duplicates)
   */
  public static <E> HashSet<E> newHashSet(E... elements) {
    int capacity = Maps.capacity(elements.length);
    HashSet<E> set = new HashSet<E>(capacity);
    Collections.addAll(set, elements);
    return set;
  }

  /**
   * Creates an empty {@code HashSet} instance with enough capacity to hold the
   * specified number of elements without rehashing.
   *
   * @param expectedSize the expected size
   * @return a new, empty {@code HashSet} with enough capacity to hold {@code
   *     expectedSize} elements without rehashing
   * @throws IllegalArgumentException if {@code expectedSize} is negative
   */
  public static <E> HashSet<E> newHashSetWithExpectedSize(int expectedSize) {
    return new HashSet<E>(Maps.capacity(expectedSize));
  }

  /**
   * Creates a <i>mutable</i> {@code HashSet} instance containing the given
   * elements in unspecified order.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are
   * non-null, use {@link ImmutableSet#copyOf(Iterable)} instead.
   *
   * <p><b>Note:</b> if {@code E} is an {@link Enum} type, use
   * {@link #newEnumSet(Iterable, Class)} instead.
   *
   * @param elements the elements that the set should contain
   * @return a new {@code HashSet} containing those elements (minus duplicates)
   */
  public static <E> HashSet<E> newHashSet(Iterable<? extends E> elements) {
    return (elements instanceof Collection)
        ? new HashSet<E>(Collections2.cast(elements))
        : newHashSet(elements.iterator());
  }

  /**
   * Creates a <i>mutable</i> {@code HashSet} instance containing the given
   * elements in unspecified order.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are
   * non-null, use {@link ImmutableSet#copyOf(Iterable)} instead.
   *
   * <p><b>Note:</b> if {@code E} is an {@link Enum} type, you should create an
   * {@link EnumSet} instead.
   *
   * @param elements the elements that the set should contain
   * @return a new {@code HashSet} containing those elements (minus duplicates)
   */
  public static <E> HashSet<E> newHashSet(Iterator<? extends E> elements) {
    HashSet<E> set = newHashSet();
    while (elements.hasNext()) {
      set.add(elements.next());
    }
    return set;
  }

  // LinkedHashSet

  /**
   * Creates a <i>mutable</i>, empty {@code LinkedHashSet} instance.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableSet#of()} instead.
   *
   * @return a new, empty {@code LinkedHashSet}
   */
  public static <E> LinkedHashSet<E> newLinkedHashSet() {
    return new LinkedHashSet<E>();
  }

  /**
   * Creates a <i>mutable</i> {@code LinkedHashSet} instance containing the
   * given elements in order.
   *
   * <p><b>Note:</b> if mutability is not required and the elements are
   * non-null, use {@link ImmutableSet#copyOf(Iterable)} instead.
   *
   * @param elements the elements that the set should contain, in order
   * @return a new {@code LinkedHashSet} containing those elements (minus
   *     duplicates)
   */
  public static <E> LinkedHashSet<E> newLinkedHashSet(
      Iterable<? extends E> elements) {
    if (elements instanceof Collection) {
      return new LinkedHashSet<E>(Collections2.cast(elements));
    }
    LinkedHashSet<E> set = newLinkedHashSet();
    for (E element : elements) {
      set.add(element);
    }
    return set;
  }

  // TreeSet

  /**
   * Creates a <i>mutable</i>, empty {@code TreeSet} instance sorted by the
   * natural sort ordering of its elements.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableSortedSet#of()} instead.
   *
   * @return a new, empty {@code TreeSet}
   */
  @SuppressWarnings("unchecked")  // allow ungenerified Comparable types
  public static <E extends Comparable> TreeSet<E> newTreeSet() {
    return new TreeSet<E>();
  }

  /**
   * Creates a <i>mutable</i> {@code TreeSet} instance containing the given
   * elements sorted by their natural ordering.
   *
   * <p><b>Note:</b> if mutability is not required, use {@link
   * ImmutableSortedSet#copyOf(Iterable)} instead.
   *
   * <p><b>Note:</b> If {@code elements} is a {@code SortedSet} with an explicit
   * comparator, this method has different behavior than
   * {@link TreeSet#TreeSet(SortedSet)}, which returns a {@code TreeSet} with
   * that comparator.
   *
   * @param elements the elements that the set should contain
   * @return a new {@code TreeSet} containing those elements (minus duplicates)
   */
  @SuppressWarnings("unchecked")  // allow ungenerified Comparable types
  public static <E extends Comparable> TreeSet<E> newTreeSet(
      Iterable<? extends E> elements) {
    TreeSet<E> set = newTreeSet();
    for (E element : elements) {
      set.add(element);
    }
    return set;
  }

  /**
   * Creates a <i>mutable</i>, empty {@code TreeSet} instance with the given
   * comparator.
   *
   * <p><b>Note:</b> if mutability is not required, use {@code
   * ImmutableSortedSet.orderedBy(comparator).build()} instead.
   *
   * @param comparator the comparator to use to sort the set
   * @return a new, empty {@code TreeSet}
   * @throws NullPointerException if {@code comparator} is null
   */
  public static <E> TreeSet<E> newTreeSet(Comparator<? super E> comparator) {
    return new TreeSet<E>(checkNotNull(comparator));
  }

  /**
   * Creates an empty {@code Set} that uses identity to determine equality. It
   * compares object references, instead of calling {@code equals}, to
   * determine whether a provided object matches an element in the set. For
   * example, {@code contains} returns {@code false} when passed an object that
   * equals a set member, but isn't the same instance. This behavior is similar
   * to the way {@link IdentityHashMap} handles key lookups.
   *
   * @since 8
   */
  public static <E> Set<E> newIdentityHashSet() {
    return Sets.newSetFromMap(Maps.<E, Boolean>newIdentityHashMap());
  }

  /**
   * Creates an {@code EnumSet} consisting of all enum values that are not in
   * the specified collection. If the collection is an {@link EnumSet}, this
   * method has the same behavior as {@link EnumSet#complementOf}. Otherwise,
   * the specified collection must contain at least one element, in order to
   * determine the element type. If the collection could be empty, use
   * {@link #complementOf(Collection, Class)} instead of this method.
   *
   * @param collection the collection whose complement should be stored in the
   *     enum set
   * @return a new, modifiable {@code EnumSet} containing all values of the enum
   *     that aren't present in the given collection
   * @throws IllegalArgumentException if {@code collection} is not an
   *     {@code EnumSet} instance and contains no elements
   */
  public static <E extends Enum<E>> EnumSet<E> complementOf(
      Collection<E> collection) {
    if (collection instanceof EnumSet) {
      return EnumSet.complementOf((EnumSet<E>) collection);
    }
    checkArgument(!collection.isEmpty(),
        "collection is empty; use the other version of this method");
    Class<E> type = collection.iterator().next().getDeclaringClass();
    return makeComplementByHand(collection, type);
  }

  /**
   * Creates an {@code EnumSet} consisting of all enum values that are not in
   * the specified collection. This is equivalent to
   * {@link EnumSet#complementOf}, but can act on any input collection, as long
   * as the elements are of enum type.
   *
   * @param collection the collection whose complement should be stored in the
   *     {@code EnumSet}
   * @param type the type of the elements in the set
   * @return a new, modifiable {@code EnumSet} initially containing all the
   *     values of the enum not present in the given collection
   */
  public static <E extends Enum<E>> EnumSet<E> complementOf(
      Collection<E> collection, Class<E> type) {
    checkNotNull(collection);
    return (collection instanceof EnumSet)
        ? EnumSet.complementOf((EnumSet<E>) collection)
        : makeComplementByHand(collection, type);
  }

  private static <E extends Enum<E>> EnumSet<E> makeComplementByHand(
      Collection<E> collection, Class<E> type) {
    EnumSet<E> result = EnumSet.allOf(type);
    result.removeAll(collection);
    return result;
  }

  /*
   * Regarding newSetForMap() and SetFromMap:
   *
   * Written by Doug Lea with assistance from members of JCP JSR-166
   * Expert Group and released to the public domain, as explained at
   * http://creativecommons.org/licenses/publicdomain
   */

  /**
   * Returns a set backed by the specified map. The resulting set displays
   * the same ordering, concurrency, and performance characteristics as the
   * backing map. In essence, this factory method provides a {@link Set}
   * implementation corresponding to any {@link Map} implementation. There is no
   * need to use this method on a {@link Map} implementation that already has a
   * corresponding {@link Set} implementation (such as {@link HashMap} or
   * {@link TreeMap}).
   *
   * <p>Each method invocation on the set returned by this method results in
   * exactly one method invocation on the backing map or its <tt>keySet</tt>
   * view, with one exception. The <tt>addAll</tt> method is implemented as a
   * sequence of <tt>put</tt> invocations on the backing map.
   *
   * <p>The specified map must be empty at the time this method is invoked,
   * and should not be accessed directly after this method returns. These
   * conditions are ensured if the map is created empty, passed directly
   * to this method, and no reference to the map is retained, as illustrated
   * in the following code fragment: <pre>  {@code
   *
   *   Set<Object> identityHashSet = Sets.newSetFromMap(
   *       new IdentityHashMap<Object, Boolean>());}</pre>
   *
   * This method has the same behavior as the JDK 6 method
   * {@code Collections.newSetFromMap()}. The returned set is serializable if
   * the backing map is.
   *
   * @param map the backing map
   * @return the set backed by the map
   * @throws IllegalArgumentException if <tt>map</tt> is not empty
   */
  public static <E> Set<E> newSetFromMap(Map<E, Boolean> map) {
    return new SetFromMap<E>(map);
  }

  private static class SetFromMap<E> extends AbstractSet<E>
      implements Set<E>, Serializable {
    private final Map<E, Boolean> m; // The backing map
    private transient Set<E> s; // Its keySet

    SetFromMap(Map<E, Boolean> map) {
      checkArgument(map.isEmpty(), "Map is non-empty");
      m = map;
      s = map.keySet();
    }

    @Override public void clear() {
      m.clear();
    }
    @Override public int size() {
      return m.size();
    }
    @Override public boolean isEmpty() {
      return m.isEmpty();
    }
    @Override public boolean contains(Object o) {
      return m.containsKey(o);
    }
    @Override public boolean remove(Object o) {
      return m.remove(o) != null;
    }
    @Override public boolean add(E e) {
      return m.put(e, Boolean.TRUE) == null;
    }
    @Override public Iterator<E> iterator() {
      return s.iterator();
    }
    @Override public Object[] toArray() {
      return s.toArray();
    }
    @Override public <T> T[] toArray(T[] a) {
      return s.toArray(a);
    }
    @Override public String toString() {
      return s.toString();
    }
    @Override public int hashCode() {
      return s.hashCode();
    }
    @Override public boolean equals(@Nullable Object object) {
      return this == object || this.s.equals(object);
    }
    @Override public boolean containsAll(Collection<?> c) {
      return s.containsAll(c);
    }
    @Override public boolean removeAll(Collection<?> c) {
      return s.removeAll(c);
    }
    @Override public boolean retainAll(Collection<?> c) {
      return s.retainAll(c);
    }

    // addAll is the only inherited implementation
    @GwtIncompatible("not needed in emulated source")
    private static final long serialVersionUID = 0;

    @GwtIncompatible("java.io.ObjectInputStream")
    private void readObject(ObjectInputStream stream)
        throws IOException, ClassNotFoundException {
      stream.defaultReadObject();
      s = m.keySet();
    }
  }

  /**
   * An unmodifiable view of a set which may be backed by other sets; this view
   * will change as the backing sets do. Contains methods to copy the data into
   * a new set which will then remain stable. There is usually no reason to
   * retain a reference of type {@code SetView}; typically, you either use it
   * as a plain {@link Set}, or immediately invoke {@link #immutableCopy} or
   * {@link #copyInto} and forget the {@code SetView} itself.
   *
   * @since 2 (imported from Google Collections Library)
   */
  public abstract static class SetView<E> extends AbstractSet<E> {
    private SetView() {} // no subclasses but our own

    /**
     * Returns an immutable copy of the current contents of this set view.
     * Does not support null elements.
     *
     * <p><b>Warning:</b> this may have unexpected results if a backing set of
     * this view uses a nonstandard notion of equivalence, for example if it is
     * a {@link TreeSet} using a comparator that is inconsistent with {@link
     * Object#equals(Object)}.
     */
    public ImmutableSet<E> immutableCopy() {
      return ImmutableSet.copyOf(this);
    }

    /**
     * Copies the current contents of this set view into an existing set. This
     * method has equivalent behavior to {@code set.addAll(this)}, assuming that
     * all the sets involved are based on the same notion of equivalence.
     *
     * @return a reference to {@code set}, for convenience
     */
    // Note: S should logically extend Set<? super E> but can't due to either
    // some javac bug or some weirdness in the spec, not sure which.
    public <S extends Set<E>> S copyInto(S set) {
      set.addAll(this);
      return set;
    }
  }

  /**
   * Returns an unmodifiable <b>view</b> of the union of two sets. The returned
   * set contains all elements that are contained in either backing set.
   * Iterating over the returned set iterates first over all the elements of
   * {@code set1}, then over each element of {@code set2}, in order, that is not
   * contained in {@code set1}.
   *
   * <p>Results are undefined if {@code set1} and {@code set2} are sets based on
   * different equivalence relations (as {@link HashSet}, {@link TreeSet}, and
   * the {@link Map#keySet} of an {@link IdentityHashMap} all are).
   *
   * <p><b>Note:</b> The returned view performs better when {@code set1} is the
   * smaller of the two sets. If you have reason to believe one of your sets
   * will generally be smaller than the other, pass it first.
   */
  public static <E> SetView<E> union(
      final Set<? extends E> set1, final Set<? extends E> set2) {
    checkNotNull(set1, "set1");
    checkNotNull(set2, "set2");

    final Set<? extends E> set2minus1 = difference(set2, set1);

    return new SetView<E>() {
      @Override public int size() {
        return set1.size() + set2minus1.size();
      }
      @Override public boolean isEmpty() {
        return set1.isEmpty() && set2.isEmpty();
      }
      @Override public Iterator<E> iterator() {
        return Iterators.unmodifiableIterator(
            Iterators.concat(set1.iterator(), set2minus1.iterator()));
      }
      @Override public boolean contains(Object object) {
        return set1.contains(object) || set2.contains(object);
      }
      @Override public <S extends Set<E>> S copyInto(S set) {
        set.addAll(set1);
        set.addAll(set2);
        return set;
      }
      @Override public ImmutableSet<E> immutableCopy() {
        return new ImmutableSet.Builder<E>()
            .addAll(set1).addAll(set2).build();
      }
    };
  }

  /**
   * Returns an unmodifiable <b>view</b> of the intersection of two sets. The
   * returned set contains all elements that are contained by both backing sets.
   * The iteration order of the returned set matches that of {@code set1}.
   *
   * <p>Results are undefined if {@code set1} and {@code set2} are sets based
   * on different equivalence relations (as {@code HashSet}, {@code TreeSet},
   * and the keySet of an {@code IdentityHashMap} all are).
   *
   * <p><b>Note:</b> The returned view performs slightly better when {@code
   * set1} is the smaller of the two sets. If you have reason to believe one of
   * your sets will generally be smaller than the other, pass it first.
   * Unfortunately, since this method sets the generic type of the returned set
   * based on the type of the first set passed, this could in rare cases force
   * you to make a cast, for example: <pre>   {@code
   *
   *   Set<Object> aFewBadObjects = ...
   *   Set<String> manyBadStrings = ...
   *
   *   // impossible for a non-String to be in the intersection
   *   SuppressWarnings("unchecked")
   *   Set<String> badStrings = (Set) Sets.intersection(
   *       aFewBadObjects, manyBadStrings);}</pre>
   *
   * This is unfortunate, but should come up only very rarely.
   */
  public static <E> SetView<E> intersection(
      final Set<E> set1, final Set<?> set2) {
    checkNotNull(set1, "set1");
    checkNotNull(set2, "set2");

    final Predicate<Object> inSet2 = Predicates.in(set2);
    return new SetView<E>() {
      @Override public Iterator<E> iterator() {
        return Iterators.filter(set1.iterator(), inSet2);
      }
      @Override public int size() {
        return Iterators.size(iterator());
      }
      @Override public boolean isEmpty() {
        return !iterator().hasNext();
      }
      @Override public boolean contains(Object object) {
        return set1.contains(object) && set2.contains(object);
      }
      @Override public boolean containsAll(Collection<?> collection) {
        return set1.containsAll(collection)
            && set2.containsAll(collection);
      }
    };
  }

  /**
   * Returns an unmodifiable <b>view</b> of the difference of two sets. The
   * returned set contains all elements that are contained by {@code set1} and
   * not contained by {@code set2}. {@code set2} may also contain elements not
   * present in {@code set1}; these are simply ignored. The iteration order of
   * the returned set matches that of {@code set1}.
   *
   * <p>Results are undefined if {@code set1} and {@code set2} are sets based
   * on different equivalence relations (as {@code HashSet}, {@code TreeSet},
   * and the keySet of an {@code IdentityHashMap} all are).
   */
  public static <E> SetView<E> difference(
      final Set<E> set1, final Set<?> set2) {
    checkNotNull(set1, "set1");
    checkNotNull(set2, "set2");

    final Predicate<Object> notInSet2 = Predicates.not(Predicates.in(set2));
    return new SetView<E>() {
      @Override public Iterator<E> iterator() {
        return Iterators.filter(set1.iterator(), notInSet2);
      }
      @Override public int size() {
        return Iterators.size(iterator());
      }
      @Override public boolean isEmpty() {
        return set2.containsAll(set1);
      }
      @Override public boolean contains(Object element) {
        return set1.contains(element) && !set2.contains(element);
      }
    };
  }

  /**
   * Returns an unmodifiable <b>view</b> of the symmetric difference of two
   * sets. The returned set contains all elements that are contained in either
   * {@code set1} or {@code set2} but not in both. The iteration order of the
   * returned set is undefined.
   *
   * <p>Results are undefined if {@code set1} and {@code set2} are sets based
   * on different equivalence relations (as {@code HashSet}, {@code TreeSet},
   * and the keySet of an {@code IdentityHashMap} all are).
   *
   * @since 3
   */
  public static <E> SetView<E> symmetricDifference(
      Set<? extends E> set1, Set<? extends E> set2) {
    checkNotNull(set1, "set1");
    checkNotNull(set2, "set2");

    // TODO(kevinb): Replace this with a more efficient implementation
    return difference(union(set1, set2), intersection(set1, set2));
  }

  /**
   * Returns the elements of {@code unfiltered} that satisfy a predicate. The
   * returned set is a live view of {@code unfiltered}; changes to one affect
   * the other.
   *
   * <p>The resulting set's iterator does not support {@code remove()}, but all
   * other set methods are supported. When given an element that doesn't satisfy
   * the predicate, the set's {@code add()} and {@code addAll()} methods throw
   * an {@link IllegalArgumentException}. When methods such as {@code
   * removeAll()} and {@code clear()} are called on the filtered set, only
   * elements that satisfy the filter will be removed from the underlying set.
   *
   * <p>The returned set isn't threadsafe or serializable, even if
   * {@code unfiltered} is.
   *
   * <p>Many of the filtered set's methods, such as {@code size()}, iterate
   * across every element in the underlying set and determine which elements
   * satisfy the filter. When a live view is <i>not</i> needed, it may be faster
   * to copy {@code Iterables.filter(unfiltered, predicate)} and use the copy.
   *
   * <p><b>Warning:</b> {@code predicate} must be <i>consistent with equals</i>,
   * as documented at {@link Predicate#apply}. Do not provide a predicate such
   * as {@code Predicates.instanceOf(ArrayList.class)}, which is inconsistent
   * with equals. (See {@link Iterables#filter(Iterable, Class)} for related
   * functionality.)
   */
  public static <E> Set<E> filter(
      Set<E> unfiltered, Predicate<? super E> predicate) {
    if (unfiltered instanceof FilteredSet) {
      // Support clear(), removeAll(), and retainAll() when filtering a filtered
      // collection.
      FilteredSet<E> filtered = (FilteredSet<E>) unfiltered;
      Predicate<E> combinedPredicate
          = Predicates.<E>and(filtered.predicate, predicate);
      return new FilteredSet<E>(
          (Set<E>) filtered.unfiltered, combinedPredicate);
    }

    return new FilteredSet<E>(
        checkNotNull(unfiltered), checkNotNull(predicate));
  }

  private static class FilteredSet<E> extends FilteredCollection<E>
      implements Set<E> {
    FilteredSet(Set<E> unfiltered, Predicate<? super E> predicate) {
      super(unfiltered, predicate);
    }

    @Override public boolean equals(@Nullable Object object) {
      return equalsImpl(this, object);
    }

    @Override public int hashCode() {
      return hashCodeImpl(this);
    }
  }

  /**
   * Returns every possible list that can be formed by choosing one element
   * from each of the given sets in order; the "n-ary
   * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
   * product</a>" of the sets. For example: <pre>   {@code
   *
   *   Sets.cartesianProduct(ImmutableList.of(
   *       ImmutableSet.of(1, 2),
   *       ImmutableSet.of("A", "B", "C")))}</pre>
   *
   * returns a set containing six lists:
   *
   * <ul>
   * <li>{@code ImmutableList.of(1, "A")}
   * <li>{@code ImmutableList.of(1, "B")}
   * <li>{@code ImmutableList.of(1, "C")}
   * <li>{@code ImmutableList.of(2, "A")}
   * <li>{@code ImmutableList.of(2, "B")}
   * <li>{@code ImmutableList.of(2, "C")}
   * </ul>
   *
   * The order in which these lists are returned is not guaranteed, however the
   * position of an element inside a tuple always corresponds to the position of
   * the set from which it came in the input list. Note that if any input set is
   * empty, the Cartesian product will also be empty. If no sets at all are
   * provided (an empty list), the resulting Cartesian product has one element,
   * an empty list (counter-intuitive, but mathematically consistent).
   *
   * <p><i>Performance notes:</i> while the cartesian product of sets of size
   * {@code m, n, p} is a set of size {@code m x n x p}, its actual memory
   * consumption is much smaller. When the cartesian set is constructed, the
   * input sets are merely copied. Only as the resulting set is iterated are the
   * individual lists created, and these are not retained after iteration.
   *
   * @param sets the sets to choose elements from, in the order that
   *     the elements chosen from those sets should appear in the resulting
   *     lists
   * @param <B> any common base class shared by all axes (often just {@link
   *     Object})
   * @return the Cartesian product, as an immutable set containing immutable
   *     lists
   * @throws NullPointerException if {@code sets}, any one of the {@code sets},
   *     or any element of a provided set is null
   * @since 2
   */
  public static <B> Set<List<B>> cartesianProduct(
      List<? extends Set<? extends B>> sets) {
    CartesianSet<B> cartesianSet = new CartesianSet<B>(sets);
    return cartesianSet.isEmpty() ? ImmutableSet.<List<B>>of() : cartesianSet;
  }

  /**
   * Returns every possible list that can be formed by choosing one element
   * from each of the given sets in order; the "n-ary
   * <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
   * product</a>" of the sets. For example: <pre>   {@code
   *
   *   Sets.cartesianProduct(
   *       ImmutableSet.of(1, 2),
   *       ImmutableSet.of("A", "B", "C"))}</pre>
   *
   * returns a set containing six lists:
   *
   * <ul>
   * <li>{@code ImmutableList.of(1, "A")}
   * <li>{@code ImmutableList.of(1, "B")}
   * <li>{@code ImmutableList.of(1, "C")}
   * <li>{@code ImmutableList.of(2, "A")}
   * <li>{@code ImmutableList.of(2, "B")}
   * <li>{@code ImmutableList.of(2, "C")}
   * </ul>
   *
   * The order in which these lists are returned is not guaranteed, however the
   * position of an element inside a tuple always corresponds to the position of
   * the set from which it came in the input list. Note that if any input set is
   * empty, the Cartesian product will also be empty. If no sets at all are
   * provided, the resulting Cartesian product has one element, an empty list
   * (counter-intuitive, but mathematically consistent).
   *
   * <p><i>Performance notes:</i> while the cartesian product of sets of size
   * {@code m, n, p} is a set of size {@code m x n x p}, its actual memory
   * consumption is much smaller. When the cartesian set is constructed, the
   * input sets are merely copied. Only as the resulting set is iterated are the
   * individual lists created, and these are not retained after iteration.
   *
   * @param sets the sets to choose elements from, in the order that
   *     the elements chosen from those sets should appear in the resulting
   *     lists
   * @param <B> any common base class shared by all axes (often just {@link
   *     Object})
   * @return the Cartesian product, as an immutable set containing immutable
   *     lists
   * @throws NullPointerException if {@code sets}, any one of the {@code sets},
   *     or any element of a provided set is null
   * @since 2
   */
  public static <B> Set<List<B>> cartesianProduct(
      Set<? extends B>... sets) {
    return cartesianProduct(Arrays.asList(sets));
  }

  private static class CartesianSet<B> extends AbstractSet<List<B>> {
    final ImmutableList<Axis> axes;
    final int size;

    CartesianSet(List<? extends Set<? extends B>> sets) {
      long dividend = 1;
      ImmutableList.Builder<Axis> builder = ImmutableList.builder();
      for (Set<? extends B> set : sets) {
        Axis axis = new Axis(set, (int) dividend); // check overflow at end
        builder.add(axis);
        dividend *= axis.size();
      }
      this.axes = builder.build();
      size = Ints.checkedCast(dividend);
    }

    @Override public int size() {
      return size;
    }

    @Override public UnmodifiableIterator<List<B>> iterator() {
      return new UnmodifiableIterator<List<B>>() {
        int index;

        @Override
        public boolean hasNext() {
          return index < size;
        }

        @Override
        public List<B> next() {
          if (!hasNext()) {
            throw new NoSuchElementException();
          }

          Object[] tuple = new Object[axes.size()];
          for (int i = 0 ; i < tuple.length; i++) {
            tuple[i] = axes.get(i).getForIndex(index);
          }
          index++;

          @SuppressWarnings("unchecked") // only B's are put in here
          List<B> result = (ImmutableList<B>) ImmutableList.copyOf(tuple);
          return result;
        }
      };
    }

    @Override public boolean contains(Object element) {
      if (!(element instanceof List<?>)) {
        return false;
      }
      List<?> tuple = (List<?>) element;
      int dimensions = axes.size();
      if (tuple.size() != dimensions) {
        return false;
      }
      for (int i = 0; i < dimensions; i++) {
        if (!axes.get(i).contains(tuple.get(i))) {
          return false;
        }
      }
      return true;
    }

    @Override public boolean equals(@Nullable Object object) {
      // Warning: this is broken if size() == 0, so it is critical that we
      // substitute an empty ImmutableSet to the user in place of this
      if (object instanceof CartesianSet) {
        CartesianSet<?> that = (CartesianSet<?>) object;
        return this.axes.equals(that.axes);
      }
      return super.equals(object);
    }

    @Override public int hashCode() {
      // Warning: this is broken if size() == 0, so it is critical that we
      // substitute an empty ImmutableSet to the user in place of this

      // It's a weird formula, but tests prove it works.
      int adjust = size - 1;
      for (int i = 0; i < axes.size(); i++) {
        adjust *= 31;
      }
      return axes.hashCode() + adjust;
    }

    private class Axis {
      final ImmutableSet<? extends B> choices;
      final ImmutableList<? extends B> choicesList;
      final int dividend;

      Axis(Set<? extends B> set, int dividend) {
        choices = ImmutableSet.copyOf(set);
        choicesList = choices.asList();
        this.dividend = dividend;
      }

      int size() {
        return choices.size();
      }

      B getForIndex(int index) {
        return choicesList.get(index / dividend % size());
      }

      boolean contains(Object target) {
        return choices.contains(target);
      }

      @SuppressWarnings("unchecked") // javac rejects "CartesianSet<?>.Axis"
      @Override public boolean equals(Object obj) {
        if (obj instanceof CartesianSet.Axis) {
          CartesianSet.Axis that = (CartesianSet.Axis) obj;
          return this.choices.equals(that.choices);
          // dividends must be equal or we wouldn't have gotten this far
        }
        return false;
      }

      @Override public int hashCode() {
        // Because Axis instances are not exposed, we can
        // opportunistically choose whatever bizarre formula happens
        // to make CartesianSet.hashCode() as simple as possible.
        return size / choices.size() * choices.hashCode();
      }
    }
  }

  /**
   * Returns the set of all possible subsets of {@code set}. For example,
   * {@code powerSet(ImmutableSet.of(1, 2))} returns the set {@code {{},
   * {1}, {2}, {1, 2}}}.
   *
   * <p>Elements appear in these subsets in the same iteration order as they
   * appeared in the input set. The order in which these subsets appear in the
   * outer set is undefined. Note that the power set of the empty set is not the
   * empty set, but a one-element set containing the empty set.
   *
   * <p>The returned set and its constituent sets use {@code equals} to decide
   * whether two elements are identical, even if the input set uses a different
   * concept of equivalence.
   *
   * <p><i>Performance notes:</i> while the power set of a set with size {@code
   * n} is of size {@code 2^n}, its memory usage is only {@code O(n)}. When the
   * power set is constructed, the input set is merely copied. Only as the
   * power set is iterated are the individual subsets created, and these subsets
   * themselves occupy only a few bytes of memory regardless of their size.
   *
   * @param set the set of elements to construct a power set from
   * @return the power set, as an immutable set of immutable sets
   * @throws IllegalArgumentException if {@code set} has more than 30 unique
   *     elements (causing the power set size to exceed the {@code int} range)
   * @throws NullPointerException if {@code set} or any of its elements is
   *     null
   * @see <a href="http://en.wikipedia.org/wiki/Power_set">Power set article at
   *      Wikipedia</a>
   * @since 4
   */
  @GwtCompatible(serializable = false)
  public static <E> Set<Set<E>> powerSet(Set<E> set) {
    ImmutableSet<E> input = ImmutableSet.copyOf(set);
    checkArgument(input.size() <= 30,
        "Too many elements to create power set: %s > 30", input.size());
    return new PowerSet<E>(input);
  }

  private static final class PowerSet<E> extends AbstractSet<Set<E>> {
    final ImmutableSet<E> inputSet;
    final ImmutableList<E> inputList;
    final int powerSetSize;

    PowerSet(ImmutableSet<E> input) {
      this.inputSet = input;
      this.inputList = input.asList();
      this.powerSetSize = 1 << input.size();
    }

    @Override public int size() {
      return powerSetSize;
    }

    @Override public boolean isEmpty() {
      return false;
    }

    @Override public Iterator<Set<E>> iterator() {
      return new AbstractIndexedListIterator<Set<E>>(powerSetSize) {
        @Override protected Set<E> get(final int setBits) {
          return new AbstractSet<E>() {
            @Override public int size() {
              return Integer.bitCount(setBits);
            }
            @Override public Iterator<E> iterator() {
              return new BitFilteredSetIterator<E>(inputList, setBits);
            }
          };
        }
      };
    }

    private static final class BitFilteredSetIterator<E>
        extends UnmodifiableIterator<E> {
      final ImmutableList<E> input;
      int remainingSetBits;

      BitFilteredSetIterator(ImmutableList<E> input, int allSetBits) {
        this.input = input;
        this.remainingSetBits = allSetBits;
      }

      @Override public boolean hasNext() {
        return remainingSetBits != 0;
      }

      @Override public E next() {
        int index = Integer.numberOfTrailingZeros(remainingSetBits);
        if (index == 32) {
          throw new NoSuchElementException();
        }

        int currentElementMask = 1 << index;
        remainingSetBits &= ~currentElementMask;
        return input.get(index);
      }
    }

    @Override public boolean contains(@Nullable Object obj) {
      if (obj instanceof Set) {
        Set<?> set = (Set<?>) obj;
        return inputSet.containsAll(set);
      }
      return false;
    }

    @Override public boolean equals(@Nullable Object obj) {
      if (obj instanceof PowerSet) {
        PowerSet<?> that = (PowerSet<?>) obj;
        return inputSet.equals(that.inputSet);
      }
      return super.equals(obj);
    }

    @Override public int hashCode() {
      /*
       * The sum of the sums of the hash codes in each subset is just the sum of
       * each input element's hash code times the number of sets that element
       * appears in. Each element appears in exactly half of the 2^n sets, so:
       */
      return inputSet.hashCode() << (inputSet.size() - 1);
    }

    @Override public String toString() {
      return "powerSet(" + inputSet + ")";
    }
  }

  /**
   * An implementation for {@link Set#hashCode()}.
   */
  static int hashCodeImpl(Set<?> s) {
    int hashCode = 0;
    for (Object o : s) {
      hashCode += o != null ? o.hashCode() : 0;
    }
    return hashCode;
  }

  /**
   * An implementation for {@link Set#equals(Object)}.
   */
  static boolean equalsImpl(Set<?> s, @Nullable Object object){
    if (s == object) {
      return true;
    }
    if (object instanceof Set) {
      Set<?> o = (Set<?>) object;

      try {
        return s.size() == o.size() && s.containsAll(o);
      } catch (NullPointerException ignored) {
        return false;
      } catch (ClassCastException ignored) {
        return false;
      }
    }
    return false;
  }

  /**
   * Creates a view of Set<B> for a Set<A>, given a bijection between A and B.
   * (Modelled for now as InvertibleFunction<A, B>, can't be Converter<A, B>
   * because that's not in Guava, though both designs are less than optimal).
   * Note that the bijection is treated as undefined for values not in the
   * given Set<A> - it doesn't have to define a true bijection for those.
   *
   * <p>Note that the returned Set's contains method is unsafe -
   * you *must* pass an instance of B to it, since the bijection
   * can only invert B's (not any Object) back to A, so we can
   * then delegate the call to the original Set<A>.
   */
  static <A, B> Set<B> transform(Set<A> set, InvertibleFunction<A, B> bijection) {
    return new TransformedSet<A, B>(
        Preconditions.checkNotNull(set, "set"),
        Preconditions.checkNotNull(bijection, "bijection")
    );
  }

  /**
   * Stop-gap measure since there is no bijection related type in Guava.
   */
  abstract static class InvertibleFunction<A, B> implements Function<A, B> {
    abstract A invert(B b);

    public InvertibleFunction<B, A> inverse() {
      return new InvertibleFunction<B, A>() {
        @Override public A apply(B b) {
          return InvertibleFunction.this.invert(b);
        }

        @Override B invert(A a) {
          return InvertibleFunction.this.apply(a);
        }

        // Not required per se, but just for good karma.
        @Override public InvertibleFunction<A, B> inverse() {
          return InvertibleFunction.this;
        }
      };
    }
  }

  private static class TransformedSet<A, B> extends AbstractSet<B> {
    final Set<A> delegate;
    final InvertibleFunction<A, B> bijection;

    TransformedSet(Set<A> delegate, InvertibleFunction<A, B> bijection) {
      this.delegate = delegate;
      this.bijection = bijection;
    }

    @Override public Iterator<B> iterator() {
      return Iterators.transform(delegate.iterator(), bijection);
    }

    @Override public int size() {
      return delegate.size();
    }

    @SuppressWarnings("unchecked") // unsafe, passed object *must* be B
    @Override public boolean contains(Object o) {
      B b = (B) o;
      A a = bijection.invert(b);
      /*
       * Mathematically, Converter<A, B> defines a bijection between ALL A's
       * on ALL B's. Here we concern ourselves with a subset
       * of this relation: we only want the part that is defined by a *subset*
       * of all A's (defined by that Set<A> delegate), and the image
       * of *that* on B (which is this set). We don't care whether
       * the converter is *not* a bijection for A's that are not in Set<A>
       * or B's not in this Set<B>.
       *
       * We only want to return true if and only f the user passes a B instance
       * that is contained in precisely in the image of Set<A>.
       *
       * The first test is whether the inverse image of this B is indeed
       * in Set<A>. But we don't know whether that B belongs in this Set<B>
       * or not; if not, the converter is free to return
       * anything it wants, even an element of Set<A> (and this relationship
       * is not part of the Set<A> <--> Set<B> bijection), and we must not
       * be confused by that. So we have to do a final check to see if the
       * image of that A is really equivalent to the passed B, which proves
       * that the given B belongs indeed in the image of Set<A>.
       */
      return delegate.contains(a) && Objects.equal(bijection.apply(a), o);
    }

    @Override public boolean add(B b) {
      return delegate.add(bijection.invert(b));
    }

    @SuppressWarnings("unchecked") // unsafe, passed object *must* be B
    @Override public boolean remove(Object o) {
      return contains(o) ? delegate.remove(bijection.invert((B) o)) : false;
    }

    @Override public void clear() {
      delegate.clear();
    }
  }
}
