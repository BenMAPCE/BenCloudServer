package gov.epa.bencloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gov.epa.bencloud.api.model.PopulationCategoryKey;

class PopulationCategoryKeyTest {

	@Test
	void equals_sameValues_returnsTrue() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(1, 2, 3, 4);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	void equals_self_returnsTrue() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		assertEquals(a, a);
	}

	@Test
	void equals_null_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		assertNotEquals(a, null);
	}

	@Test
	void equals_differentClass_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		assertNotEquals(a, "not a key");
	}

	@Test
	void equals_differentAgeRange_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(99, 2, 3, 4);
		assertNotEquals(a, b);
	}

	@Test
	void equals_differentRace_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(1, 99, 3, 4);
		assertNotEquals(a, b);
	}

	@Test
	void equals_differentEthnicity_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(1, 2, 99, 4);
		assertNotEquals(a, b);
	}

	@Test
	void equals_differentGender_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(1, 2, 3, 99);
		assertNotEquals(a, b);
	}

	/**
	 * HIFTaskRunnable uses {@code new PopulationCategoryKey(popAgeRange, null, null, null)}
	 * to look up incidence/prevalence by age only. Two such keys with the same age must
	 * compare equal; with different ages, unequal.
	 */
	@Test
	void equals_nullStratifiers_sameAge_returnsTrue() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, null, null, null);
		PopulationCategoryKey b = new PopulationCategoryKey(1, null, null, null);
		assertEquals(a, b);
	}

	@Test
	void equals_nullStratifiers_differentAge_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, null, null, null);
		PopulationCategoryKey b = new PopulationCategoryKey(2, null, null, null);
		assertNotEquals(a, b);
	}

	@Test
	void equals_oneFieldNullOnOnlyOneSide_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(1, null, 3, 4);
		assertNotEquals(a, b);
		assertNotEquals(b, a);
	}

	/**
	 * Regression test for the equals-via-hashcode bug. {@code Objects.hash(1,2,3,4)}
	 * and {@code Objects.hash(0,33,3,4)} both equal 955331 (their walks converge at
	 * step 2 and stay equal). A field-based equals must distinguish them; a hash-based
	 * equals will incorrectly merge them.
	 */
	@Test
	void equals_collidingHashes_butDifferentFields_returnsFalse() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(0, 33, 3, 4);
		assertEquals(a.hashCode(), b.hashCode(), "fixture precondition: hashes must collide");
		assertNotEquals(a, b, "distinct keys must not be merged just because their hashes collide");
	}

	@Test
	void hashCode_equalKeys_haveEqualHashes() {
		PopulationCategoryKey a = new PopulationCategoryKey(1, 2, 3, 4);
		PopulationCategoryKey b = new PopulationCategoryKey(1, 2, 3, 4);
		assertTrue(a.equals(b) && a.hashCode() == b.hashCode());
	}
}
