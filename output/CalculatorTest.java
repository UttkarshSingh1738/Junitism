package com.junitism.testsubjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import com.junitism.testsubjects.Calculator;

class CalculatorTest {

    @Test
    @DisplayName("test0")
    void test0() {
        com.junitism.testsubjects.Calculator var0 = new com.junitism.testsubjects.Calculator();
        int var1 = 43;
        int var2 = var0.divide(var1, var1);
        assertEquals(1, var2);
    }

    @Test
    @DisplayName("test1")
    void test1() {
        com.junitism.testsubjects.Calculator var0 = new com.junitism.testsubjects.Calculator();
        int var1 = 1;
        int var2 = var0.divide(var1, var1);
        int var3 = var0.add(var2, var2);
        int var4 = var0.subtract(var3, var1);
        boolean var5 = var0.isPositive(var3);
        int var6 = var0.subtract(var2, var4);
        boolean var7 = var0.isPositive(var2);
        int var8 = var0.subtract(var2, var1);
        boolean var9 = var0.isPositive(var3);
        int var10 = var0.add(var1, var2);
        int var11 = var0.divide(var10, var8);
        boolean var12 = var0.isPositive(var2);
        int var13 = var0.add(var1, var2);
        int var14 = var0.subtract(var6, var10);
        int var15 = var0.add(var6, var8);
    }

}
