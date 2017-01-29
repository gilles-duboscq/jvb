#!/usr/bin/env python

# <configuration default="false" name="pong" type="Application" factoryName="Application">
#   <option name="MAIN_CLASS_NAME" value="gd.twohundred.jvb.Main" />
#   <option name="VM_PARAMETERS" value="" />
#   <option name="PROGRAM_PARAMETERS" value="&quot;$PROJECT_DIR$/../vb-roms/all-roms/pong.vb&quot;" />
#   <option name="WORKING_DIRECTORY" value="file://$PROJECT_DIR$" />
#   <option name="ALTERNATIVE_JRE_PATH_ENABLED" value="false" />
#   <option name="ALTERNATIVE_JRE_PATH" />
#   <option name="ENABLE_SWING_INSPECTOR" value="false" />
#   <option name="ENV_VARIABLES" />
#   <option name="PASS_PARENT_ENVS" value="true" />
#   <module name="jvb" />
#   <envs />
#   <method />
# </configuration>

from __future__ import print_function
from xml.etree import ElementTree
from argparse import ArgumentParser
from os.path import dirname, basename, exists, join, realpath, relpath
from glob import glob
import shlex

scripts = realpath(dirname(__file__))
name = basename(__file__)
root = dirname(scripts)
default_idea_dir = None
if exists(join(root, '.idea')):
    default_idea_dir = join(root, '.idea')

parser = ArgumentParser(name, description="Creates run configurations for Intellij for *.vb files in a directory")
parser.add_argument("-p", "--project", default=default_idea_dir, help="Location of .idea project", required=default_idea_dir is None)
parser.add_argument("directory", help="directory where to look for *.vb files")


args = parser.parse_args()

workspace_file = join(args.project, 'workspace.xml')
if not exists(workspace_file):
    print("No workspace.xml file, creating one")
    project = ElementTree.Element('project', attrib={'version': 4})
    doc = ElementTree.ElementTree(project)
else:
    doc = ElementTree.parse(workspace_file)
    project = doc.getroot()
    if project.tag != 'project':
        print("Strange workspace.xml file, exiting")
        exit(1)

search_dir = realpath(args.directory)
vb_roms = set(glob(join(search_dir, "*.vb")))

run_manager = project.find("./component[@name='RunManager']")
if run_manager is None:
    run_manager = ElementTree.SubElement(project, 'component')
    run_manager.set('name', 'RunManager')

project_dir = dirname(realpath(args.project))

for config in run_manager.iterfind("./configuration[@type='Application']"):
    params_node = config.find("./option[@name='PROGRAM_PARAMETERS']")
    if params_node is None or params_node.get('value') is None:
        continue
    params = shlex.split(params_node.get('value'))
    for param in params:
        param = param.replace('$PROJECT_DIR$', project_dir)
        if not param.endswith('.vb'):
            continue
        param = realpath(param)
        if param in vb_roms:
            vb_roms.remove(param)
            print("Skipping '" + relpath(param, search_dir) + "'")

for vb_rom in vb_roms:
    print("Adding '" + relpath(vb_rom, search_dir) + "'")
    config = ElementTree.SubElement(run_manager, 'configuration', attrib={"name": relpath(vb_rom, search_dir)[:-3], "type": "Application", "factoryName": "Application"})
    ElementTree.SubElement(config, 'option', attrib={'name': 'MAIN_CLASS_NAME', 'value': 'gd.twohundred.jvb.Main'})
    ElementTree.SubElement(config, 'option', attrib={'name': 'VM_PARAMETERS', 'value': ''})
    ElementTree.SubElement(config, 'option', attrib={'name': 'PROGRAM_PARAMETERS', 'value': '"$PROJECT_DIR$/' + relpath(vb_rom, project_dir) + '"'})
    ElementTree.SubElement(config, 'option', attrib={'name': 'WORKING_DIRECTORY', 'value': 'file://$PROJECT_DIR$'})
    ElementTree.SubElement(config, 'option', attrib={'name': 'PASS_PARENT_ENVS', 'value': 'true'})
    ElementTree.SubElement(config, 'module', attrib={'name': 'jvb'})

doc.write(workspace_file, xml_declaration=True)
