const {
  listConfluenceLinks,
  getField,
  getPages,
  getPagesById,
  getFilterFields,
  getValues
} = require('./api');

const FIELD1 = 10107;

describe('API Tests', () => {
  beforeAll(async () => {});

  it('List confluence links', async () => {
    const links = await listConfluenceLinks();
    expect(links.length).toBe(1);
    expect(links[0].name).toBe('Confluence');
  });

  it('Get field', async () => {
    const field = await getField(FIELD1);
    expect(field.type.key).toBe('com.mesilat.confluence-fields:confluence-field');
    expect(field.type.name).toBe('Confluence Field');
  });

  it('Get pages for project TP', async () => {
    const pages = await getPages('TP', FIELD1);
    expect(pages.start).toBe(0);
    expect(pages.totalSize).toBe(100);
    //expect(pages.limit).toBe(25);
  });

  it('Get pages for project AP', async () => {
    const pages = await getPages('AP', FIELD1);
    expect(pages.start).toBe(0);
    expect(pages.totalSize).toBe(7);
  });

  it('Get pages non-existing project', async () => {
    const pages = await getPages('FAKE', FIELD1);
    expect(pages.start).toBe(0);
    expect(pages.totalSize).toBe(100);
  });

  it('Get pages for query', async () => {
    const pages = await getPages('AP', FIELD1, { q: 'IBM' });
    expect(pages.start).toBe(0);
    expect(pages.totalSize).toBe(1);
  });

  it('Get pages with limit', async () => {
    const pages = await getPages('TP', FIELD1, { 'max-results': 10 });
    expect(pages.start).toBe(0);
    expect(pages.size).toBe(10);
  });

  it('Get pages with filter', async () => {
    const pages = await getPages('YP', FIELD1, { 'filter-fields': JSON.stringify({ space: 'RD2' }) });
    expect(pages.start).toBe(0);
    expect(pages.size).toBe(7);
  });

  it('Get pages with invalid filter results in HTTP 500', async () => {
    try {
      const pages = await getPages('YP', FIELD1, { 'filter-fields': JSON.stringify({ space: 'XXX' }) });
    } catch(err) {
      expect(err.message).toBe('Internal Server Error');
    }
  });

  it('Get pages by id', async () => {
    const pages = await getPagesById('TP', FIELD1, { 'page-id': 65934 });
    expect(pages.size).toBe(1);
    expect(pages.results[0].title).toBe('IBM');
  });

  it('Get filter fields', async () => {
    const filterFields = await getFilterFields('YP', FIELD1);
    expect(filterFields.length).toBe(1);
    expect(filterFields[0]).toBe('space');
  });

  it('Get values', async () => {
    const values = await getValues(FIELD1);
    expect(values.filter(value => value.title === 'Apple').length).toBe(2);
  });

/*
  it('Get fields', async () => {
    try {
      const fields = await getFields();
      console.debug(fields);
    } catch (err) {
      console.error(err);
    }
  });
*/
});
