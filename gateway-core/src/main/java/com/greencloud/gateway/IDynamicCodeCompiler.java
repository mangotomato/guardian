package com.greencloud.gateway;

import java.io.File;

/**
 * Dynamic code compiler
 *
 * @author leejianhao
 */
public interface IDynamicCodeCompiler {
    Class compile(String sCode, String sName) throws Exception;

    Class compile(File file) throws Exception;
}
