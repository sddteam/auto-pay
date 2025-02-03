# @capacitor/auto-pay

auto pay

## Install

```bash
npm install @capacitor/auto-pay
npx cap sync
```

## API

<docgen-index>

* [`echo(...)`](#echo)
* [`performGesture(...)`](#performgesture)
* [`navigateGCash(...)`](#navigategcash)
* [`stopNavigation(...)`](#stopnavigation)
* [`checkAccessibility()`](#checkaccessibility)
* [`enableAccessibility()`](#enableaccessibility)
* [`addListener('warning', ...)`](#addlistenerwarning-)
* [`addListener('error', ...)`](#addlistenererror-)
* [`removeAllListeners()`](#removealllisteners)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------


### performGesture(...)

```typescript
performGesture(options: { x: number; y: number; }) => Promise<any>
```

| Param         | Type                                   |
| ------------- | -------------------------------------- |
| **`options`** | <code>{ x: number; y: number; }</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### navigateGCash(...)

```typescript
navigateGCash(options: any) => Promise<any>
```

| Param         | Type             |
| ------------- | ---------------- |
| **`options`** | <code>any</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### stopNavigation(...)

```typescript
stopNavigation(options: any) => Promise<any>
```

| Param         | Type             |
| ------------- | ---------------- |
| **`options`** | <code>any</code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### checkAccessibility()

```typescript
checkAccessibility() => Promise<any>
```

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### enableAccessibility()

```typescript
enableAccessibility() => Promise<any>
```

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### addListener('warning', ...)

```typescript
addListener(eventName: 'warning', listenerFunc: (data: { message: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                 |
| ------------------ | ---------------------------------------------------- |
| **`eventName`**    | <code>'warning'</code>                               |
| **`listenerFunc`** | <code>(data: { message: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### addListener('error', ...)

```typescript
addListener(eventName: 'error', listenerFunc: (data: { message: string; }) => void) => Promise<PluginListenerHandle>
```

| Param              | Type                                                 |
| ------------------ | ---------------------------------------------------- |
| **`eventName`**    | <code>'error'</code>                                 |
| **`listenerFunc`** | <code>(data: { message: string; }) =&gt; void</code> |

**Returns:** <code>Promise&lt;<a href="#pluginlistenerhandle">PluginListenerHandle</a>&gt;</code>

--------------------


### removeAllListeners()

```typescript
removeAllListeners() => Promise<void>
```

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |

</docgen-api>
