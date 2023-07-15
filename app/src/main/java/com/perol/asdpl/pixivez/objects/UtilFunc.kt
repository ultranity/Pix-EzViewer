package com.perol.asdpl.pixivez.objects

import java.util.IdentityHashMap
import java.util.SortedSet


/**
 * Returns Null if the two specified collections have no
 * elements in common. else the first common element.
 *
 *
 * Care must be exercised if this method is used on collections that
 * do not comply with the general contract for `Collection`.
 * Implementations may elect to iterate over either collection and test
 * for containment in the other collection (or to perform any equivalent
 * computation).  If either collection uses a nonstandard equality test
 * (as does a [SortedSet] whose ordering is not *compatible with
 * equals*, or the key set of an [IdentityHashMap]), both
 * collections must use the same nonstandard equality test, or the
 * result of this method is undefined.
 *
 *
 * Care must also be exercised when using collections that have
 * restrictions on the elements that they may contain. Collection
 * implementations are allowed to throw exceptions for any operation
 * involving elements they deem ineligible. For absolute safety the
 * specified collections should contain only elements which are
 * eligible elements for both collections.
 *
 *
 * Note that it is permissible to pass the same collection in both
 * parameters, in which case the method will return `true` if and
 * only if the collection is empty.
 *
 * @param c1 a collection
 * @param c2 a collection
 * @return `true` if the two specified collections have no
 * elements in common.
 * @throws NullPointerException if either collection is `null`.
 * @throws NullPointerException if one collection contains a `null`
 * element and `null` is not an eligible element for the other collection.
 * ([optional](Collection.html#optional-restrictions))
 * @throws ClassCastException if one collection contains an element that is
 * of a type which is ineligible for the other collection.
 * ([optional](Collection.html#optional-restrictions))
 * @since 1.5
 */
fun <T:Any?> firstCommon(c1: Collection<T>, c2: Collection<T>): T? {
    // The collection to be used for contains(). Preference is given to
    // the collection who's contains() has lower O() complexity.
    var contains = c2
    // The collection to be iterated. If the collections' contains() impl
    // are of different O() complexity, the collection with slower
    // contains() will be used for iteration. For collections who's
    // contains() are of the same complexity then best performance is
    // achieved by iterating the smaller collection.
    var iterate = c1

    // Performance optimization cases. The heuristics:
    //   1. Generally iterate over c1.
    //   2. If c1 is a Set then iterate over c2.
    //   3. If either collection is empty then result is always true.
    //   4. Iterate over the smaller Collection.
    if (c1 is Set<*>) {
        // Use c1 for contains as a Set's contains() is expected to perform
        // better than O(N/2)
        iterate = c2
        contains = c1
    } else if (c2 !is Set<*>) {
        // Both are mere Collections. Iterate over smaller collection.
        // Example: If c1 contains 3 elements and c2 contains 50 elements and
        // assuming contains() requires ceiling(N/2) comparisons then
        // checking for all c1 elements in c2 would require 75 comparisons
        // (3 * ceiling(50/2)) vs. checking all c2 elements in c1 requiring
        // 100 comparisons (50 * ceiling(3/2)).
        val c1size = c1.size
        val c2size = c2.size
        if (c1size == 0 || c2size == 0) {
            // At least one collection is empty. Nothing will match.
            return null
        }
        if (c1size > c2size) {
            iterate = c2
            contains = c1
        }
    }
    for (e in iterate) {
        if (contains.contains(e)) {
            // Found a common element. Collections are not disjoint.
            return e
        }
    }

    // No common elements were found.
    return null
}
