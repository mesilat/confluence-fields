import { JIRA_HOME } from './components/constants';
import loginForm from './components/login-form';
import issueConfiguration from './components/issue-configuration';

describe('Plugin Tests', () => {
  beforeAll(async () => {
    await page.goto(`${JIRA_HOME}`);
  });
  beforeEach(async () => {
    jest.setTimeout(30000);
  });

  it('a user can login using form', loginForm);
  it('a user can websudo to confluence fields general settings page', issueConfiguration);
});
