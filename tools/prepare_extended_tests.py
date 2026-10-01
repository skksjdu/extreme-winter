"""Prepare isolated production-JAR regression resources; never touch an installed instance."""
from pathlib import Path
import argparse, json
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser()
parser.add_argument('--mode',choices=['benchmark','survival','legacy-create','legacy-upgrade'],required=True)
parser.add_argument('--attempt',type=int,default=1)
args=parser.parse_args()
names={'benchmark':'WinterBenchmarkTest','survival':'WinterSurvivalRegressionTest','legacy-create':'WinterLegacySaveTest','legacy-upgrade':'WinterUpgradeTest'}
work=root/'work/e-validation';resources=work/f'{args.mode}-resources';resources.mkdir(parents=True,exist_ok=True)
meta={'schemaVersion':1,'id':'winter_extended_test','version':'1.0.0','name':'Extreme Winter isolated extended regressions','environment':'client','entrypoints':{'fabric-client-gametest':[f'dev.extremewinter.test.{names[args.mode]}']}}
if args.mode=='benchmark':
    meta['mixins']=['winter-benchmark.mixins.json']
    mixins={'required':True,'package':'dev.extremewinter.test.profile','compatibilityLevel':'JAVA_25','mixins':['InstanceWorkMixin','StaticWorkMixin','InventoryWorkMixin','FarmingWorkMixin'],'injectors':{'defaultRequire':1}}
    (resources/'winter-benchmark.mixins.json').write_text(json.dumps(mixins,indent=2)+'\n',encoding='utf-8')
(resources/'fabric.mod.json').write_text(json.dumps(meta,indent=2)+'\n',encoding='utf-8')
run=work/f'run-{args.mode}-{args.attempt}';(run/'config').mkdir(parents=True,exist_ok=True)
if args.mode=='legacy-create':
    config={'configVersion':2,'coldVanillaBiomes':True,'coldModdedBiomes':False,'maxSnowLayers':0,'persistentWeather':True}
    (run/'config/extreme-winter.json').write_text(json.dumps(config,indent=2)+'\n',encoding='utf-8')
elif not (run/'config/extreme-winter.json').exists():
    config=json.loads((root/'outputs/releases/26.0.6/extreme-winter-26.0.6-target-config.json').read_text(encoding='utf-8'));config['coldModdedBiomes']=False
    (run/'config/extreme-winter.json').write_text(json.dumps(config,indent=2)+'\n',encoding='utf-8')
(work/'review.gradle').write_text('''gradle.projectsEvaluated {
    rootProject.with {
        def mode = providers.gradleProperty('winterExtended').get()
        def attempt = providers.gradleProperty('winterAttempt').getOrElse('1')
        def resources = file("work/e-validation/${mode}-resources")
        tasks.register('winterExtendedJar', Jar) {
            from(sourceSets.gametest.output) { include 'dev/extremewinter/test/**' }
            from resources
            archiveClassifier = "${mode}-test"
            destinationDirectory = layout.buildDirectory.dir('testmods')
        }
        tasks.register('runWinterExtended', tasks.named('runProductionGameTest').get().getClass().superclass) {
            def winterJar = mode == 'legacy-create' ? file('outputs/releases/26.0.2/extreme-winter-26.0.2.jar') : tasks.named('jar')
            mods.setFrom(winterJar, tasks.named('winterExtendedJar'), configurations.productionRuntimeMods)
            runDir = file("work/e-validation/run-${mode}-${attempt}")
            jvmArgs.addAll('-Dfabric.client.gametest',
                "-Dfabric.client.gametest.testModResourcesPath=${resources.absolutePath}",
                "-Dwinter.test.metricsPath=${file('work/e-validation/benchmark-metrics.json').absolutePath}",
                "-Dwinter.test.skipClockSegments=${providers.gradleProperty('winterSkipClock').getOrElse('false')}",
                "-Dwinter.test.legacyWorld=${file('work/e-validation/legacy-world-' + attempt).absolutePath}")
            programArgs.addAll('--username','Player0','--version',project.minecraft_version,'--accessToken','0')
        }
    }
}
''',encoding='utf-8')
print(json.dumps({'mode':args.mode,'attempt':args.attempt,'resources':str(resources),'run':str(run)},indent=2))
