package main.objects;

public enum ErrorCode {
	NULL,
	/*
	 * Request Construction Error
	 */
	E0000,
	E0001,
	E0010,
	E0011,
	E0012,
	E0020,
	E0021,
	E0022,
	E0100,
	E0101,
	
	/*
	 * Modular Error
	 */
	E1000,
	
	/*
	 * Transaction Error
	 */
	E2000,
	E2001,
	E2010,
	E2011,
	E2012,
	E2013,
	E2014,
	E2021,
	E2022;
	/*E00000,
	E00001,
	E00002,
	E01000,
	E01001,
	E01002,
	E10000,
	E10001,
	E11000,
	E11001,
	E11002,
	E11100,
	E12000,
	E12001,
	E12002,
	E12003,
	E12004,
	E12005,
	E12006,
	E12007,
	E12008,
	E12009,
	E12010,
	E12011,
	E12012,
	E12100,
	E12101,
	E12102,
	E12103,
	E12104,
	E70000;*/
	
	private String code;
	private String description;
	
	/*ErrorCode(String description) {
		setDescription(description);
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		if(this.equals(NULL)) {
			return toString();
		} else {
			return toString().substring(1);
		}
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}
}
