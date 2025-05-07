# @capacitor/auto-pay

auto pay

## Install

```bash
npm install @capacitor/auto-pay
npx cap sync
```

## API

<docgen-index>

* [`checkAccessibility()`](#checkaccessibility)
* [`enableAccessibility()`](#enableaccessibility)
* [`checkOverlayPermission()`](#checkoverlaypermission)
* [`enableOverlayPermission()`](#enableoverlaypermission)
* [`addListener('warning', ...)`](#addlistenerwarning-)
* [`addListener('error', ...)`](#addlistenererror-)
* [`removeAllListeners()`](#removealllisteners)
* [`startAutoPay(...)`](#startautopay)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

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


### checkOverlayPermission()

```typescript
checkOverlayPermission() => Promise<any>
```

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### enableOverlayPermission()

```typescript
enableOverlayPermission() => Promise<any>
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


### startAutoPay(...)

```typescript
startAutoPay(options: PayMateOptions) => Promise<any>
```

| Param         | Type                                                      |
| ------------- | --------------------------------------------------------- |
| **`options`** | <code><a href="#paymateoptions">PayMateOptions</a></code> |

**Returns:** <code>Promise&lt;any&gt;</code>

--------------------


### Interfaces


#### PluginListenerHandle

| Prop         | Type                                      |
| ------------ | ----------------------------------------- |
| **`remove`** | <code>() =&gt; Promise&lt;void&gt;</code> |


#### PayMateOptions

| Prop         | Type                |
| ------------ | ------------------- |
| **`app`**    | <code>string</code> |
| **`base64`** | <code>string</code> |

</docgen-api>
