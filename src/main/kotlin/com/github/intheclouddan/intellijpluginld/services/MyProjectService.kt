package com.github.intheclouddan.intellijpluginld.services

import com.intellij.openapi.project.Project
import com.github.intheclouddan.intellijpluginld.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
