package gov.epa.bencloud.api.model;

import java.util.Objects;

public class PopulationCategoryKey{
	private final int ageRangeId;
	private final int raceId;
	private final int ethnicityId;
	private final int genderId;
	private final int hashcode;
	
	public PopulationCategoryKey(int ageRangeId, int raceId, int ethnicityId, int genderId) {
		this.ageRangeId = ageRangeId;
		this.raceId = raceId;
		this.ethnicityId = ethnicityId;
		this.genderId = genderId;
		this.hashcode = Objects.hash(ageRangeId, raceId, ethnicityId, genderId);
	}

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PopulationCategoryKey that = (PopulationCategoryKey) o;
        return hashcode == that.hashCode();
    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }
    
	/**
	 * @return the ageRangeId
	 */
	public int getAgeRangeId() {
		return ageRangeId;
	}

	/**
	 * @return the raceId
	 */
	public int getRaceId() {
		return raceId;
	}

	/**
	 * @return the ethnicityId
	 */
	public int getEthnicityId() {
		return ethnicityId;
	}

	/**
	 * @return the genderId
	 */
	public int getGenderId() {
		return genderId;
	}

	/**
	 * @return the hashcode
	 */
	public int getHashcode() {
		return hashcode;
	}
	
	
}