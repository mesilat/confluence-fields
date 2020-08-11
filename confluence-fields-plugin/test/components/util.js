import { JIRA_HOME, JIRA_USER, JIRA_PASSWORD } from './constants';
import { exec } from 'child_process';
import axios from 'axios';
import kill from 'tree-kill';

export const delay = async (time) =>
   new Promise(resolve => setTimeout(resolve, time));

// not tested
export async function waitForJiraToStart(){
  let text = [];
  return new Promise((resolve, reject) => {
    const server = exec('atlas-run', { // atlas-run-standalone --product jira --context-path /jira -p 2990
      cwd: `${process.cwd()}/..`,
      shell: '/bin/bash',
    });
    server.stdout.on('data', (data) => {
      const d = data.split(/\r?\n/);
      if (d.length > 0){
        text.push(d[0]);
      }
      for (let i = 1; i < d.length; i++){
        //console.log(`stdout: ${text.join('')}`);
        if (text.join('').indexOf('jira development started successfully') >= 0){
          resolve(server);
        }
        text = [];
        text.push(d[i]);
      }
    });
    server.stderr.on('data', (data) => {});
    server.on('close', (code) => {
      reject();
    });
  });
}

// not tested
export async function waitForJiraToStop(server){
  server.kill();
  return new Promise((resolve) => {
    const server = exec("ps -ef | grep '2990/jira' | grep -v grep | awk '{print $2}'", (err, stdout, stderr) => {
      kill(stdout.trim(), 'SIGKILL');
      resolve();
    });
  });
}
