package gov.epa.bencloud.api.model;

import java.util.Objects;

public class PopulationCategoryKey{
	private final Integer ageRangeId;
	private final Integer raceId;
	private final Integer ethnicityId;
	private final Integer genderId;
	private final Integer hashcode;
	
	public PopulationCategoryKey(Integer ageRangeId, Integer raceId, Integer ethnicityId, Integer genderId) {
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
	public Integer getAgeRangeId() {
		return ageRangeId;
	}

	/**
	 * @return the raceId
	 */
	public Integer getRaceId() {
		return raceId;
	}

	/**
	 * @return the ethnicityId
	 */
	public Integer getEthnicityId() {
		return ethnicityId;
	}

	/**
	 * @return the genderId
	 */
	public Integer getGenderId() {
		return genderId;
	}

	/**
	 * @return the hashcode
	 */
	public Integer getHashcode() {
		return hashcode;
	}
	
	
}