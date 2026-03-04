package com.junitism.maven;

import com.junitism.analysis.ClasspathScanner;
import com.junitism.ga.DynaMOSA;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Mojo(name = "generate")
public class GenerateMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(property = "junitism.targets", defaultValue = "")
    private String targets;

    @Parameter(property = "junitism.outputDir", defaultValue = "${project.build.directory}/generated-test-sources/junitism")
    private String outputDir;

    @Parameter(property = "junitism.budget", defaultValue = "60")
    private long budgetSeconds;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (targets == null || targets.isBlank()) {
            getLog().info("No targets specified, skipping");
            return;
        }

        try {
            List<Path> classpath = new ArrayList<>();
            classpath.add(project.getBuild().getOutputDirectory() != null
                    ? Path.of(project.getBuild().getOutputDirectory())
                    : Path.of("target/classes"));
            project.getArtifacts().forEach(a -> {
                if (a.getFile() != null) classpath.add(a.getFile().toPath());
            });

            DynaMOSA dyna = new DynaMOSA();
            dyna.setTimeBudgetMs(budgetSeconds * 1000);

            for (String target : targets.split(",")) {
                target = target.trim();
                if (!target.isEmpty()) {
                    getLog().info("Generating tests for " + target);
                    dyna.run(classpath, target);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Junitism generation failed", e);
        }
    }
}
