import { WebPlugin } from '@capacitor/core';

import type { AutoPayPlugin } from './definitions';

export class AutoPayWeb extends WebPlugin implements AutoPayPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
