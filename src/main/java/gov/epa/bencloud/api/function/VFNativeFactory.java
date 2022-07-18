package gov.epa.bencloud.api.function;

public class VFNativeFactory {

    public static VFNative create(String functionalForm) {
        if (functionalForm == null || functionalForm.isEmpty()) {
            return null;
        }

        if (functionalForm.equalsIgnoreCase(VFNative1.functionalForm)) {
            return new VFNative1();
        } else if (functionalForm.equalsIgnoreCase(VFNative2.functionalForm)) {
            return new VFNative2();
        } else if (functionalForm.equalsIgnoreCase(VFNative3.functionalForm)) {
            return new VFNative3();
        } else if (functionalForm.equalsIgnoreCase(VFNative4.functionalForm)) {
            return new VFNative4();
        } else if (functionalForm.equalsIgnoreCase(VFNative5.functionalForm)) {
            return new VFNative5();
        } else if (functionalForm.equalsIgnoreCase(VFNative6.functionalForm)) {
            return new VFNative6();
        } else if (functionalForm.equalsIgnoreCase(VFNative7.functionalForm)) {
            return new VFNative7();
        } else if (functionalForm.equalsIgnoreCase(VFNative8.functionalForm)) {
            return new VFNative8();
        } else if (functionalForm.equalsIgnoreCase(VFNative9.functionalForm)) {
            return new VFNative9();
        }
        
        return null;
    }    
}
