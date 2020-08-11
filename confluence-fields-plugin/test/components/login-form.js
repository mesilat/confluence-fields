import { JIRA_USER, JIRA_PASSWORD } from './constants';
import { delay } from './util';

export default async () => {
  await page.waitForSelector('#login-form-username');
  delay(100);
  await page.evaluate((username, password) => {
    document.querySelector('#login-form-username').value = username;
    document.querySelector('#login-form-password').value = password;
  }, JIRA_USER, JIRA_PASSWORD);
  await page.waitForSelector('#login');
  await page.evaluate(() => {
    document.querySelector('#login').click();
  });
  await page.waitForSelector('#dashboard');
};
