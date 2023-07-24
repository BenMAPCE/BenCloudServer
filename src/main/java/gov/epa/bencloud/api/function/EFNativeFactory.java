package gov.epa.bencloud.api.function;

public class EFNativeFactory {

    /**
     * 
     * @param functionalForm
     * @return an EFNative object for the given functional form.
     */
    public static EFNative create(String functionalForm) {
        if (functionalForm == null || functionalForm.isEmpty()) {
            return null;
        }

        if (functionalForm.equalsIgnoreCase(EFNative1.functionalForm)) {
            return new EFNative1();
        } else if (functionalForm.equalsIgnoreCase(EFNative2.functionalForm)) {
            return new EFNative2();
        }
        
        return null;
    }

}
