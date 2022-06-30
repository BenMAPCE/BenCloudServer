package gov.epa.bencloud.api.function;

public class VFNativeFactory {

    public static VFNative create(String functionalForm) {
        if (functionalForm == null || functionalForm.isEmpty()) {
            return null;
        }

        if (functionalForm.equalsIgnoreCase(VFNative1.functionalForm)) {
            return new VFNative1();
        }  

        /* TODO
         * 
A*MedicalCostIndex
A*MedicalCostIndex*B
A*MedicalCostIndex+B*((median_income)/(52*5))*WageIndex
A*B*AllGoodsIndex
A*AllGoodsIndex
A*AllGoodsIndex*B
((median_income)/(52*5))*WageIndex
A*WageIndex
         */
        
        return null;
    }    
}
