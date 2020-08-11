import { JIRA_HOME, JIRA_USER, JIRA_PASSWORD, CLIENT_FIELD, CLIENTS_FIELD } from './constants';
import { delay } from './util';

export default async () => {
  await page.goto(`${JIRA_HOME}/secure/admin/ViewCustomFields.jspa`);

  // login
  await page.waitForSelector('#login-form-username');
  delay(100);
  await page.evaluate((username, password) => {
    document.querySelector('#login-form-username').value = username;
    document.querySelector('#login-form-password').value = password;
  }, JIRA_USER, JIRA_PASSWORD);
  await page.waitForSelector('#login-form-submit');
  await page.evaluate(() => {
    document.querySelector('#login-form-submit').click();
  });

  // requires websudo
  await page.waitForSelector('#login-form-authenticatePassword');
  delay(100);
  await page.evaluate((password) => {
    document.querySelector('#login-form-authenticatePassword').value = password;
  }, JIRA_PASSWORD);
  await page.waitForSelector('#login-form-submit');
  await page.evaluate(() => {
    document.querySelector('#login-form-submit').click();
  });
  await page.waitForSelector('#customfields-container');

  // Client customer field exists
  await page.waitForSelector(`tr[data-custom-field-id="${CLIENT_FIELD}"]`);
  let title = await page.evaluate((CLIENT_FIELD) =>
    document.querySelector(`tr[data-custom-field-id="${CLIENT_FIELD}"] strong[original-title]`).textContent,
    CLIENT_FIELD
  );
  expect(title).toBe('Client');

  // Clients customer field exists
  await page.waitForSelector(`tr[data-custom-field-id="${CLIENTS_FIELD}"]`);
  title = await page.evaluate((CLIENTS_FIELD) =>
    document.querySelector(`tr[data-custom-field-id="${CLIENTS_FIELD}"] strong[original-title]`).textContent,
    CLIENTS_FIELD
  );
  expect(title).toBe('Clients');

  // Can navigate to Client field config page
  await page.goto(`${JIRA_HOME}/secure/admin/ConfigureCustomField!default.jspa?customFieldId=${CLIENT_FIELD}`);
  await page.waitForSelector('#com-mesilat-confluence-field-configure');
  await page.evaluate(() =>
    document.querySelector('#com-mesilat-confluence-field-configure').click()
  );

  await page.waitForSelector('#ml-configure-confluenceLink');
  let isSelect2 = await page.evaluate(() =>
    document.querySelector('#ml-configure-confluenceLink').classList.contains('select2-offscreen')
  );
  expect(isSelect2).toBe(true);

  await page.waitForSelector('#ml-configure-filter');

  await page.waitForSelector('#ml-configure-test');
  isSelect2 = await page.evaluate(() =>
    document.querySelector('#ml-configure-test').classList.contains('select2-offscreen')
  );
  expect(isSelect2).toBe(true);

  await page.waitForSelector('#ml-configure-multiselect');
  await page.waitForSelector('#ml-configure-autofilter');
  await page.waitForSelector('#ml-configure-asdefiner');

};
