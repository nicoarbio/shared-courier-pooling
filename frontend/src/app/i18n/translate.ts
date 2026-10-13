import { Pipe, PipeTransform, Service, inject } from '@angular/core';
import { Messages, es } from './es';

/** Every dotted path to a text in the dictionary, e.g. 'merchant.newOrder.title'. A wrong key does not compile. */
export type TranslationKey = Leaves<Messages>;

type Leaves<T, Prefix extends string = ''> = {
  [K in keyof T & string]: T[K] extends string ? `${Prefix}${K}` : Leaves<T[K], `${Prefix}${K}.`>;
}[keyof T & string];

export type TranslationParams = Record<string, string | number>;

/** Resolves texts by key. Single language for now; another one is a new `Messages` file selected here. */
@Service()
export class Translations {
  private readonly messages: Messages = es;

  t(key: TranslationKey, params?: TranslationParams): string {
    const text = key.split('.').reduce<unknown>((node, part) => (node as Record<string, unknown>)[part], this.messages);
    if (typeof text !== 'string') return key;
    return params ? text.replace(/\{(\w+)\}/g, (match, name: string) => String(params[name] ?? match)) : text;
  }
}

/** `{{ 'merchant.orders.title' | t }}` or `{{ 'courier.rounds.summary' | t: { deliveries: 5 } }}` */
@Pipe({ name: 't' })
export class TranslatePipe implements PipeTransform {
  private readonly translations = inject(Translations);

  transform(key: TranslationKey, params?: TranslationParams): string {
    return this.translations.t(key, params);
  }
}
